package com.time.coworking.repository;

import com.time.coworking.exception.DataAccessException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {

    /** Convenience overload — opens its own connection (auto-commit). */
    public void log(String username, String action, String entity, String entityId) {
        try (Connection conn = Database.getConnection()) {
            log(conn, username, action, entity, entityId);
        } catch (SQLException e) {
            throw new DataAccessException("audit log (auto) failed", e);
        }
    }

    /** Uses the caller's connection so the audit row participates in the same transaction. */
    public void log(Connection conn, String username, String action,
                    String entity, String entityId) {
        String sql = """
                INSERT INTO audit_logs (username, action, entity, entity_id)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, action);
            ps.setString(3, entity);
            ps.setString(4, entityId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("audit log failed", e);
        }
    }

    // ─────────── READ ───────────

    /** All entries, newest first. */
    public List<AuditRow> findAll() {
        return search(null, null, null, null);
    }

    /** Filtered search: username, action, entity (all optional), and date range. */
    public List<AuditRow> search(String username, String action, String entity,
                                 LocalDate date) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, username, action, entity, entity_id, details, created_at
                FROM audit_logs
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isBlank()) {
            sql.append(" AND LOWER(username) LIKE ?");
            params.add("%" + username.toLowerCase() + "%");
        }
        if (action != null && !action.isBlank()) {
            sql.append(" AND LOWER(action) LIKE ?");
            params.add("%" + action.toLowerCase() + "%");
        }
        if (entity != null && !entity.isBlank()) {
            sql.append(" AND LOWER(entity) = ?");
            params.add(entity.toLowerCase());
        }
        if (date != null) {
            sql.append(" AND DATE(created_at) = ?");
            params.add(Date.valueOf(date));
        }

        sql.append(" ORDER BY created_at DESC LIMIT 1000");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditRow> out = new ArrayList<>();
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    out.add(new AuditRow(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("action"),
                            rs.getString("entity"),
                            rs.getString("entity_id"),
                            rs.getString("details"),
                            ts == null ? null : ts.toLocalDateTime()));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("audit search failed", e);
        }
    }

    /** Simple row record — the raw data from a single audit_logs row. */
    public record AuditRow(
            long id,
            String username,
            String action,
            String entity,
            String entityId,
            String details,
            LocalDateTime createdAt) {}
}