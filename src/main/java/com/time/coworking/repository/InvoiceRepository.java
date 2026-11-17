package com.time.coworking.repository;

import com.time.coworking.enums.InvoiceStatus;
import com.time.coworking.exception.DataAccessException;
import com.time.coworking.model.Invoice;
import com.time.coworking.model.InvoiceItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvoiceRepository {

    // ─────────── INSERT ───────────

    public long insert(Connection conn, Invoice inv) {
        String sql = """
                INSERT INTO invoices
                    (invoice_number, member_id, issue_date, subtotal, discount, total, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, inv.getInvoiceNumber());
            ps.setLong(2, inv.getMemberId());
            ps.setDate(3, Date.valueOf(inv.getIssueDate()));
            ps.setBigDecimal(4, inv.getSubtotal());
            ps.setBigDecimal(5, inv.getDiscount());
            ps.setBigDecimal(6, inv.getTotal());
            ps.setString(7, inv.getStatus().name());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) { inv.setId(k.getLong(1)); return inv.getId(); }
            }
            return 0L;
        } catch (SQLException e) { throw new DataAccessException("insert invoice failed", e); }
    }

    public void insertItem(Connection conn, InvoiceItem item) {
        String sql = """
                INSERT INTO invoice_items
                    (invoice_id, description, quantity, unit_price, line_total)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, item.getInvoiceId());
            ps.setString(2, item.getDescription());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitPrice());
            ps.setBigDecimal(5, item.getLineTotal());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("insert invoice item failed", e); }
    }

    // ─────────── READ ───────────

    public Optional<Invoice> findById(long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM invoices WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DataAccessException("findById invoice failed", e); }
    }

    public List<Invoice> findAll() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM invoices ORDER BY issue_date DESC, id DESC");
             ResultSet rs = ps.executeQuery()) {
            List<Invoice> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DataAccessException("findAll invoices failed", e); }
    }

    public List<Invoice> findByMember(long memberId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM invoices WHERE member_id = ? ORDER BY issue_date DESC")) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DataAccessException("findByMember invoices failed", e); }
    }

    public List<Invoice> search(java.time.LocalDate from, java.time.LocalDate to, InvoiceStatus status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM invoices WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (from != null)   { sql.append(" AND issue_date >= ?"); params.add(Date.valueOf(from)); }
        if (to != null)     { sql.append(" AND issue_date <= ?"); params.add(Date.valueOf(to)); }
        if (status != null) { sql.append(" AND status = ?");      params.add(status.name()); }

        sql.append(" ORDER BY issue_date DESC, id DESC");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DataAccessException("search invoices failed", e); }
    }

    public List<InvoiceItem> findItemsByInvoice(long invoiceId) {
        String sql = "SELECT * FROM invoice_items WHERE invoice_id = ? ORDER BY id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                List<InvoiceItem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new InvoiceItem(
                            rs.getLong("id"),
                            rs.getLong("invoice_id"),
                            rs.getString("description"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price"),
                            rs.getBigDecimal("line_total")));
                }
                return out;
            }
        } catch (SQLException e) { throw new DataAccessException("findItems failed", e); }
    }

    public void updateStatus(Connection conn, long id, InvoiceStatus status) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE invoices SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("updateStatus invoice failed", e); }
    }

    private Invoice map(ResultSet rs) throws SQLException {
        return new Invoice(
                rs.getLong("id"),
                rs.getString("invoice_number"),
                rs.getLong("member_id"),
                rs.getDate("issue_date").toLocalDate(),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                InvoiceStatus.valueOf(rs.getString("status")));
    }
}