package com.time.coworking.repository;

import com.time.coworking.enums.PaymentMethod;
import com.time.coworking.enums.PaymentStatus;
import com.time.coworking.exception.DataAccessException;
import com.time.coworking.model.Payment;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentRepository {

    public Optional<Payment> findById(long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM payments WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DataAccessException("findById payment failed", e); }
    }

    public long insert(Connection conn, Payment p) {
        String sql = """
                INSERT INTO payments
                    (member_id, invoice_id, amount, payment_date, method, status, reference_no)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.getMemberId());
            if (p.getInvoiceId() != null) ps.setLong(2, p.getInvoiceId());
            else ps.setNull(2, Types.BIGINT);
            ps.setBigDecimal(3, p.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(
                    p.getPaymentDate() == null ? LocalDateTime.now() : p.getPaymentDate()));
            ps.setString(5, p.getMethod().name());
            ps.setString(6, p.getStatus().name());
            ps.setString(7, p.getReferenceNo());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) { p.setId(k.getLong(1)); return p.getId(); }
            }
            return 0L;
        } catch (SQLException e) { throw new DataAccessException("insert payment failed", e); }
    }

    public List<Payment> findAll() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM payments ORDER BY payment_date DESC");
             ResultSet rs = ps.executeQuery()) {
            List<Payment> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DataAccessException("findAll payments failed", e); }
    }

    public List<Payment> findByMember(long memberId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM payments WHERE member_id = ? ORDER BY payment_date DESC")) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Payment> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DataAccessException("findByMember payments failed", e); }
    }

    public List<Payment> search(Long memberId, java.time.LocalDate from, java.time.LocalDate to,
                                PaymentStatus status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM payments WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (memberId != null) { sql.append(" AND member_id = ?"); params.add(memberId); }
        if (from != null)     { sql.append(" AND payment_date >= ?"); params.add(Timestamp.valueOf(from.atStartOfDay())); }
        if (to != null)       { sql.append(" AND payment_date < ?"); params.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay())); }
        if (status != null)   { sql.append(" AND status = ?"); params.add(status.name()); }

        sql.append(" ORDER BY payment_date DESC");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<Payment> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DataAccessException("search payments failed", e); }
    }

    public void updateStatus(Connection conn, long id, PaymentStatus status) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE payments SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("updateStatus payment failed", e); }
    }

    public BigDecimal sumPaidBetween(java.time.LocalDate from, java.time.LocalDate to) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) FROM payments
                WHERE status = 'PAID'
                  AND payment_date >= ?
                  AND payment_date < ?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal v = rs.getBigDecimal(1);
                return v == null ? BigDecimal.ZERO : v;
            }
        } catch (SQLException e) { throw new DataAccessException("sumPaid failed", e); }
    }

    private Payment map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("payment_date");
        Long invId = (Long) rs.getObject("invoice_id");
        return new Payment(
                rs.getLong("id"),
                rs.getLong("member_id"),
                invId,
                rs.getBigDecimal("amount"),
                ts == null ? LocalDateTime.now() : ts.toLocalDateTime(),
                PaymentMethod.valueOf(rs.getString("method")),
                PaymentStatus.valueOf(rs.getString("status")),
                rs.getString("reference_no"));
    }
}