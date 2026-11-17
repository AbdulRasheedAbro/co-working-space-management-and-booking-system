package com.time.coworking.service;

import com.time.coworking.Session;
import com.time.coworking.enums.BookingStatus;
import com.time.coworking.exception.PaymentException;
import com.time.coworking.model.Booking;
import com.time.coworking.model.CheckIn;
import com.time.coworking.model.Workspace;
import com.time.coworking.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class CheckInService {

    private final CheckInRepository     checkInRepo = new CheckInRepository();
    private final BookingRepository     bookingRepo = new BookingRepository();
    private final WorkspaceRepository   workspaceRepo = new WorkspaceRepository();
    private final AuditLogRepository    auditRepo   = new AuditLogRepository();

    public List<CheckIn> findAll()  { return checkInRepo.findAll(); }
    public List<CheckIn> findToday() { return checkInRepo.findToday(); }

    /**
     * Records a check-in for the given booking.
     * Sets booking status to CHECKED_IN.
     */
    public CheckIn checkIn(long bookingId) {
        return Database.inTransaction(conn -> {
            Booking b = bookingRepo.findById(conn, bookingId)
                    .orElseThrow(() -> new PaymentException("Booking #" + bookingId + " not found"));

            if (b.getStatus() == BookingStatus.CHECKED_IN) {
                throw new PaymentException("Booking is already checked in");
            }
            if (b.getStatus() == BookingStatus.CANCELLED
                    || b.getStatus() == BookingStatus.NO_SHOW
                    || b.getStatus() == BookingStatus.COMPLETED) {
                throw new PaymentException(
                        "Cannot check in — booking status is " + b.getStatus());
            }

            if (checkInRepo.findByBooking(conn, bookingId).isPresent()) {
                throw new PaymentException("Booking already has a check-in record");
            }

            CheckIn c = new CheckIn(0, bookingId);
            c.setCheckinTime(LocalDateTime.now());
            c.setExtraCharge(BigDecimal.ZERO);
            checkInRepo.insert(conn, c);

            bookingRepo.updateStatus(conn, bookingId, BookingStatus.CHECKED_IN);
            auditRepo.log(conn, Session.getUsername(), "Checked in",
                    "Booking", b.getBookingCode());
            return c;
        });
    }

    /**
     * Records a check-out, computes actual minutes and any extra charge
     * (hourly rate × overtime if the member stayed past the booked end).
     * Sets booking status to COMPLETED.
     */
    public CheckIn checkOut(long bookingId) {
        return Database.inTransaction(conn -> {
            Booking b = bookingRepo.findById(conn, bookingId)
                    .orElseThrow(() -> new PaymentException("Booking #" + bookingId + " not found"));

            CheckIn c = checkInRepo.findByBooking(conn, bookingId)
                    .orElseThrow(() -> new PaymentException("No check-in found for this booking"));

            if (c.getCheckinTime() == null) {
                throw new PaymentException("Check-in time is missing");
            }
            if (c.getCheckoutTime() != null) {
                throw new PaymentException("Booking is already checked out");
            }

            LocalDateTime out = LocalDateTime.now();
            c.setCheckoutTime(out);

            long actualMin = Duration.between(c.getCheckinTime(), out).toMinutes();
            c.setActualMin((int) actualMin);

            long bookedMin = b.getDurationMin();
            BigDecimal extra = BigDecimal.ZERO;
            if (actualMin > bookedMin) {
                long extraMin = actualMin - bookedMin;
                extra = calculateExtraCharge(conn, b.getWorkspaceId(), extraMin);
            }
            c.setExtraCharge(extra);

            checkInRepo.update(conn, c);
            bookingRepo.updateStatus(conn, bookingId, BookingStatus.COMPLETED);
            auditRepo.log(conn, Session.getUsername(), "Checked out",
                    "Booking", b.getBookingCode());
            return c;
        });
    }

    /** Returns the check-in record for the booking (or null). */
    public CheckIn findByBooking(long bookingId) {
        return checkInRepo.findByBooking(bookingId).orElse(null);
    }

    private BigDecimal calculateExtraCharge(Connection conn, long workspaceId, long extraMinutes)
            throws SQLException {
        Workspace ws = workspaceRepo.findById(conn, workspaceId).orElse(null);
        if (ws == null) return BigDecimal.ZERO;

        BigDecimal hourly = null;
        if (ws instanceof com.time.coworking.model.MeetingRoom mr) {
            hourly = mr.getHourlyPrice();
        } else {
            // For desks/offices, use the monthly price divided by 30*8 = 240 hours (approximation)
            BigDecimal monthly = ws.getPrice();
            if (monthly != null) {
                hourly = monthly.divide(BigDecimal.valueOf(240), 2, RoundingMode.HALF_UP);
            }
        }
        if (hourly == null) return BigDecimal.ZERO;

        return hourly.multiply(BigDecimal.valueOf(extraMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}