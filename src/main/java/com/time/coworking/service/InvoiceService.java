package com.time.coworking.service;

import com.time.coworking.enums.InvoiceStatus;
import com.time.coworking.exception.DataAccessException;
import com.time.coworking.model.Booking;
import com.time.coworking.model.Invoice;
import com.time.coworking.model.InvoiceItem;
import com.time.coworking.repository.InvoiceRepository;
import com.time.coworking.util.InvoiceNumberGenerator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class InvoiceService {

    private final InvoiceRepository repo = new InvoiceRepository();

    // ─────────── Create (used by BookingService) ───────────

    public Invoice createForBooking(Connection conn, Booking booking, String workspaceCode)
            throws SQLException {
        BigDecimal amount = booking.getAmount() == null ? BigDecimal.ZERO : booking.getAmount();

        Invoice inv = new Invoice(
                0,
                InvoiceNumberGenerator.next(),
                booking.getMemberId(),
                LocalDate.now(),
                amount,
                BigDecimal.ZERO,
                amount,
                InvoiceStatus.ISSUED);

        repo.insert(conn, inv);

        String desc = String.format("Booking %s — %s (%d min)",
                booking.getBookingCode(), workspaceCode, booking.getDurationMin());
        InvoiceItem item = InvoiceItem.of(inv.getId(), desc, 1, amount);
        repo.insertItem(conn, item);

        return inv;
    }

    public void markPaid(Connection conn, long invoiceId) {
        repo.updateStatus(conn, invoiceId, InvoiceStatus.PAID);
    }

    // ─────────── Read ───────────

    public List<Invoice> findAll() {
        return repo.findAll();
    }

    public List<Invoice> search(LocalDate from, LocalDate to, InvoiceStatus status) {
        return repo.search(from, to, status);
    }

    public List<Invoice> findByMember(long memberId) {
        return repo.findByMember(memberId);
    }

    public Invoice findById(long id) {
        return repo.findById(id).orElseThrow(() ->
                new DataAccessException("Invoice #" + id + " not found", null));
    }

    public List<InvoiceItem> findItems(long invoiceId) {
        return repo.findItemsByInvoice(invoiceId);
    }
}