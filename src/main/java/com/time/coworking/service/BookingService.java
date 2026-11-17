package com.time.coworking.service;

import com.time.coworking.Session;
import com.time.coworking.dto.BookingRequest;
import com.time.coworking.enums.BookingStatus;
import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.exception.BookingConflictException;
import com.time.coworking.exception.DataAccessException;
import com.time.coworking.exception.MemberNotFoundException;
import com.time.coworking.exception.WorkspaceUnavailableException;
import com.time.coworking.model.*;
import com.time.coworking.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class BookingService {

    private final BookingRepository     bookingRepo     = new BookingRepository();
    private final WorkspaceRepository   workspaceRepo   = new WorkspaceRepository();
    private final MemberRepository      memberRepo      = new MemberRepository();
    private final InvoiceService        invoiceService  = new InvoiceService();
    private final PaymentService        paymentService  = new PaymentService();
    private final AuditLogRepository    auditRepo       = new AuditLogRepository();

    // ─────────── Read ───────────

    public List<Booking> findAll() { return bookingRepo.findAll(); }
    public List<Booking> findToday() { return bookingRepo.findToday(); }

    public List<Booking> search(LocalDate from, LocalDate to,
                                Long memberId, Long workspaceId, BookingStatus status) {
        return bookingRepo.search(from, to, memberId, workspaceId, status);
    }

    public Booking findById(long id) {
        return bookingRepo.findById(id).orElseThrow(() ->
                new WorkspaceUnavailableException("Booking #" + id + " not found"));
    }

    // ─────────── ⭐ Create — ONE ROW PER BOOKING ⭐ ───────────

    public List<Booking> create(BookingRequest req, String username) {

        if (req.startDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Cannot book a past date");
        if (req.endDate().isBefore(req.startDate()))
            throw new IllegalArgumentException("End date must be on or after start date");
        if (!req.endTime().isAfter(req.startTime()))
            throw new IllegalArgumentException("End time must be after start time");
        if (req.dayCount() > 365)
            throw new IllegalArgumentException("Booking range cannot exceed 365 days");

        return Database.inTransaction(conn -> {

            Member member = memberRepo.findById(conn, req.memberId())
                    .orElseThrow(() -> new MemberNotFoundException(
                            "Member #" + req.memberId() + " not found"));
            if (member.getStatus() != com.time.coworking.enums.MemberStatus.ACTIVE)
                throw new IllegalArgumentException("Member is not active");

            Workspace ws = workspaceRepo.findById(conn, req.workspaceId())
                    .orElseThrow(() -> new WorkspaceUnavailableException(
                            "Workspace #" + req.workspaceId() + " not found"));
            if (ws.getStatus() == WorkspaceStatus.MAINTENANCE
                    || ws.getStatus() == WorkspaceStatus.UNAVAILABLE)
                throw new WorkspaceUnavailableException(
                        "Workspace " + ws.getCode() + " is not bookable (status: " + ws.getStatus() + ")");

            // ⭐ CONFLICT DETECTION over the range
            long rangeConflicts = bookingRepo.countOverlappingRange(
                    conn, ws.getId(), req.startDate(), req.endDate(),
                    req.startTime(), req.endTime());
            if (rangeConflicts > 0)
                throw new BookingConflictException(
                        "This workspace is already booked for one or more days in the selected range.");

            long minutes = Duration.between(req.startTime(), req.endTime()).toMinutes();
            if (minutes <= 0)
                throw new IllegalArgumentException("Duration must be at least 1 minute");

            long days = req.dayCount();
            BigDecimal totalAmount = calculateTotalAmount(ws, minutes, days);

            // ⭐ ONE booking row for the entire date range
            String code = "BK-" + (System.nanoTime() % 10000000000L);

            Booking booking = new Booking(
                    0, code, member.getId(), ws.getId(),
                    req.startDate(), req.endDate(),
                    req.startTime(), req.endTime(),
                    (int) minutes, BookingStatus.CONFIRMED,
                    totalAmount, null);
            bookingRepo.insert(conn, booking);

            // ONE invoice for the whole booking
            Invoice invoice = invoiceService.createForBooking(conn, booking, ws.getCode());

            if (req.payNow() && req.paymentMethod() != null) {
                paymentService.recordForInvoice(conn, invoice, req.paymentMethod(), null);
                invoiceService.markPaid(conn, invoice.getId());
            }

            if (ws instanceof Office || ws instanceof MeetingRoom) {
                workspaceRepo.updateStatus(conn, ws.getId(), WorkspaceStatus.RESERVED);
            }

            String action = days == 1
                    ? "Created booking"
                    : "Created " + days + "-day booking";
            auditRepo.log(conn, username, action,
                    "Booking", booking.getBookingCode());

            return Collections.singletonList(booking);
        });
    }

    // ─────────── Cancel ───────────

    public void cancel(long id) {
        Database.inTransaction(conn -> {
            Booking b = bookingRepo.findById(conn, id)
                    .orElseThrow(() -> new WorkspaceUnavailableException(
                            "Booking #" + id + " not found"));
            bookingRepo.updateStatus(conn, id, BookingStatus.CANCELLED);

            Workspace ws = workspaceRepo.findById(conn, b.getWorkspaceId()).orElse(null);
            if (ws != null && ws.getStatus() == WorkspaceStatus.RESERVED) {
                String checkSql = """
                        SELECT COUNT(*) FROM bookings
                        WHERE workspace_id = ?
                          AND status IN ('PENDING','CONFIRMED','CHECKED_IN')
                        """;
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setLong(1, ws.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getLong(1) == 0) {
                            workspaceRepo.updateStatus(conn, ws.getId(), WorkspaceStatus.AVAILABLE);
                        }
                    }
                } catch (SQLException e) {
                    throw new DataAccessException("cancel release failed", e);
                }
            }

            auditRepo.log(conn, Session.getUsername(), "Cancelled booking",
                    "Booking", b.getBookingCode());
            return null;
        });
    }

    // ─────────── Pricing ───────────

    /**
     * Total price for the full range:
     *   - Meeting Room: hourly × hours × days
     *   - Desks/Offices: monthly ÷ 30 × days
     */
    private BigDecimal calculateTotalAmount(Workspace ws, long minutes, long days) {
        if (ws instanceof MeetingRoom mr) {
            BigDecimal hourly = mr.getHourlyPrice();
            if (hourly == null) return BigDecimal.ZERO;
            BigDecimal perDay = hourly.multiply(BigDecimal.valueOf(minutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            return perDay.multiply(BigDecimal.valueOf(days))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal monthly = ws.getPrice();
        if (monthly == null) return BigDecimal.ZERO;
        return monthly.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }
}