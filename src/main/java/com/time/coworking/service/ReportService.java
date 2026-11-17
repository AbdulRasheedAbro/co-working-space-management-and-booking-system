package com.time.coworking.service;

import com.time.coworking.dto.ReportRow;
import com.time.coworking.repository.Database;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class ReportService {

    // ─────────────────────────────────────────────────────────
    // 1. DAILY BOOKING REPORT
    // ─────────────────────────────────────────────────────────
    public List<ReportRow> dailyBookingReport(LocalDate from, LocalDate to) {
        return Database.inTransaction(conn -> {
            String sql = """
                    SELECT
                        b.booking_date,
                        COUNT(*)                          AS total_bookings,
                        COUNT(DISTINCT b.workspace_id)    AS distinct_resources,
                        COUNT(CASE WHEN b.status = 'CHECKED_IN' THEN 1 END) AS checkins,
                        COUNT(CASE WHEN b.status = 'COMPLETED'  THEN 1 END) AS completed,
                        COUNT(CASE WHEN b.status = 'CANCELLED'  THEN 1 END) AS cancellations,
                        COUNT(CASE WHEN b.status = 'NO_SHOW'    THEN 1 END) AS no_shows,
                        SUM(b.amount)                     AS revenue
                    FROM bookings b
                    WHERE b.booking_date BETWEEN ? AND ?
                    GROUP BY b.booking_date
                    ORDER BY b.booking_date DESC
                    """;
            return executeQuery(conn, sql, List.of(Date.valueOf(from), Date.valueOf(to)));
        });
    }

    // ─────────────────────────────────────────────────────────
    // 2. MEMBER REPORT
    // ─────────────────────────────────────────────────────────
    public List<ReportRow> memberReport() {
        return Database.inTransaction(conn -> {
            String sql = """
                    SELECT
                        m.id                                    AS member_id,
                        m.full_name,
                        m.email,
                        m.company,
                        m.membership_type,
                        m.status,
                        m.join_date,
                        m.expiry_date,
                        COUNT(DISTINCT b.id)                    AS total_bookings,
                        COALESCE(SUM(b.amount), 0)              AS total_booking_value,
                        COUNT(DISTINCT p.id)                    AS total_payments,
                        COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amount END), 0) AS total_paid
                    FROM members m
                    LEFT JOIN bookings b ON b.member_id = m.id
                    LEFT JOIN payments p ON p.member_id = m.id
                    GROUP BY m.id, m.full_name, m.email, m.company,
                             m.membership_type, m.status, m.join_date, m.expiry_date
                    ORDER BY total_paid DESC
                    """;
            return executeQuery(conn, sql, List.of());
        });
    }

    // ─────────────────────────────────────────────────────────
    // 3. REVENUE REPORT (daily)
    // ─────────────────────────────────────────────────────────
    public List<ReportRow> revenueReport(LocalDate from, LocalDate to) {
        return Database.inTransaction(conn -> {
            String sql = """
                    SELECT
                        DATE(p.payment_date) AS payment_day,
                        COUNT(*)             AS payment_count,
                        SUM(p.amount)        AS total_revenue,
                        AVG(p.amount)        AS avg_payment
                    FROM payments p
                    WHERE p.status = 'PAID'
                      AND DATE(p.payment_date) BETWEEN ? AND ?
                    GROUP BY DATE(p.payment_date)
                    HAVING SUM(p.amount) > 0
                    ORDER BY payment_day DESC
                    """;
            return executeQuery(conn, sql, List.of(Date.valueOf(from), Date.valueOf(to)));
        });
    }

    // ─────────────────────────────────────────────────────────
    // 4. WORKSPACE UTILIZATION REPORT
    // ─────────────────────────────────────────────────────────
    public List<ReportRow> utilizationReport(LocalDate from, LocalDate to) {
        return Database.inTransaction(conn -> {
            String sql = """
                    SELECT
                        w.code                              AS workspace_code,
                        w.type                              AS workspace_type,
                        w.floor,
                        w.zone,
                        w.status                            AS current_status,
                        COUNT(b.id)                         AS total_bookings,
                        COALESCE(SUM(b.duration_min), 0)    AS total_minutes,
                        COALESCE(SUM(b.duration_min), 0) / 60 AS total_hours,
                        COALESCE(SUM(b.amount), 0)          AS total_revenue
                    FROM workspaces w
                    LEFT JOIN bookings b
                        ON b.workspace_id = w.id
                       AND b.booking_date BETWEEN ? AND ?
                       AND b.status IN ('CONFIRMED','CHECKED_IN','COMPLETED')
                    GROUP BY w.id, w.code, w.type, w.floor, w.zone, w.status
                    ORDER BY total_bookings DESC, w.code
                    """;
            return executeQuery(conn, sql, List.of(Date.valueOf(from), Date.valueOf(to)));
        });
    }

    // ─────────────────────────────────────────────────────────
    // Helper — executes any SELECT and returns rows as ReportRow
    // ─────────────────────────────────────────────────────────
    private List<ReportRow> executeQuery(Connection conn, String sql, List<Object> params)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    String label = meta.getColumnLabel(i);
                    columns.add(label == null || label.isBlank() ? meta.getColumnName(i) : label);
                }

                List<ReportRow> out = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        Object v = rs.getObject(i);
                        row.put(columns.get(i - 1), v);
                    }
                    out.add(new ReportRow(row));
                }
                return out;
            }
        }
    }
}