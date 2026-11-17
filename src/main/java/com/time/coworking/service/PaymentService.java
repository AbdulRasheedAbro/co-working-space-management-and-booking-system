package com.time.coworking.service;

import com.time.coworking.Session;
import com.time.coworking.enums.PaymentMethod;
import com.time.coworking.enums.PaymentStatus;
import com.time.coworking.exception.PaymentException;
import com.time.coworking.model.Invoice;
import com.time.coworking.model.Payment;
import com.time.coworking.repository.AuditLogRepository;
import com.time.coworking.repository.Database;
import com.time.coworking.repository.PaymentRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentService {

    private final PaymentRepository repo = new PaymentRepository();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    // ─────────── Read ───────────

    public List<Payment> findAll() {
        return repo.findAll();
    }

    public List<Payment> search(Long memberId, LocalDate from, LocalDate to, PaymentStatus status) {
        return repo.search(memberId, from, to, status);
    }

    public List<Payment> findByMember(long memberId) {
        return repo.findByMember(memberId);
    }

    public Payment findById(long id) {
        return repo.findById(id).orElseThrow(() ->
                new PaymentException("Payment #" + id + " not found"));
    }

    // ─────────── Write ───────────

    /** Record a payment tied to an invoice (used inside booking transaction). */
    public Payment recordForInvoice(Connection conn, Invoice invoice,
                                    PaymentMethod method, String referenceNo) throws SQLException {
        if (invoice.getTotal() == null || invoice.getTotal().signum() <= 0) {
            throw new PaymentException("Invoice total must be positive");
        }
        Payment p = new Payment(
                0,
                invoice.getMemberId(),
                invoice.getId(),
                invoice.getTotal(),
                LocalDateTime.now(),
                method == null ? PaymentMethod.CASH : method,
                PaymentStatus.PAID,
                referenceNo);
        repo.insert(conn, p);
        auditRepo.log(conn, Session.getUsername(), "Recorded payment",
                "Payment", String.valueOf(p.getId()));
        return p;
    }

    /** Standalone payment — own transaction. */
    public Payment record(long memberId, Long invoiceId, BigDecimal amount,
                          PaymentMethod method, String referenceNo) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentException("Amount must be positive");
        }
        if (method == null) {
            throw new PaymentException("Payment method is required");
        }
        return Database.inTransaction(conn -> {
            Payment p = new Payment(
                    0,
                    memberId,
                    invoiceId,
                    amount,
                    LocalDateTime.now(),
                    method,
                    PaymentStatus.PAID,
                    referenceNo);
            repo.insert(conn, p);
            auditRepo.log(conn, Session.getUsername(), "Recorded payment",
                    "Payment", String.valueOf(p.getId()));
            return p;
        });
    }

    public void cancel(long id) {
        Database.inTransaction(conn -> {
            Payment p = repo.findById(id).orElseThrow(() ->
                    new PaymentException("Payment #" + id + " not found"));
            if (p.getStatus() == PaymentStatus.PAID) {
                throw new PaymentException("Cannot cancel a completed payment — issue a refund instead");
            }
            repo.updateStatus(conn, id, PaymentStatus.CANCELLED);
            auditRepo.log(conn, Session.getUsername(), "Cancelled payment",
                    "Payment", String.valueOf(id));
            return null;
        });
    }

    public void refund(long id) {
        Database.inTransaction(conn -> {
            Payment p = repo.findById(id).orElseThrow(() ->
                    new PaymentException("Payment #" + id + " not found"));
            if (p.getStatus() != PaymentStatus.PAID) {
                throw new PaymentException("Only PAID payments can be refunded");
            }
            repo.updateStatus(conn, id, PaymentStatus.REFUNDED);
            auditRepo.log(conn, Session.getUsername(), "Refunded payment",
                    "Payment", String.valueOf(id));
            return null;
        });
    }

    // ─────────── Reporting ───────────

    public BigDecimal totalPaidBetween(LocalDate from, LocalDate to) {
        return repo.sumPaidBetween(from, to);
    }
}