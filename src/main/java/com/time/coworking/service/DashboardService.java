package com.time.coworking.service;

import com.time.coworking.dto.DashboardStats;
import com.time.coworking.enums.WorkspaceType;
import com.time.coworking.repository.Database;
import com.time.coworking.repository.WorkspaceRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;

public class DashboardService {

    private final WorkspaceRepository workspaceRepo = new WorkspaceRepository();

    public DashboardStats getStats() {
        return Database.inTransaction(conn -> {

            LocalDate today = LocalDate.now();
            LocalTime slotStart = LocalTime.of(9, 0);
            LocalTime slotEnd   = LocalTime.of(18, 0);

            long totalMembers     = scalarLong(conn, "SELECT COUNT(*) FROM members");
            long activeMembers    = scalarLong(conn, "SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'");
            long expiredMembers   = scalarLong(conn, "SELECT COUNT(*) FROM members WHERE status = 'EXPIRED'");

            long totalDesksCount  = scalarLong(conn,
                    "SELECT COUNT(*) FROM workspaces WHERE type IN ('HOT_DESK','DEDICATED_DESK')");

            long availableDesksToday =
                    workspaceRepo.countAvailableForSlot(WorkspaceType.HOT_DESK, today, slotStart, slotEnd)
                            + workspaceRepo.countAvailableForSlot(WorkspaceType.DEDICATED_DESK, today, slotStart, slotEnd);
            long occupiedDesksToday = totalDesksCount - availableDesksToday;

            long totalOfficesCount = scalarLong(conn,
                    "SELECT COUNT(*) FROM workspaces WHERE type = 'OFFICE'");
            long availableOfficesToday = workspaceRepo.countAvailableForSlot(
                    WorkspaceType.OFFICE, today, slotStart, slotEnd);
            long occupiedOfficesToday = totalOfficesCount - availableOfficesToday;

            long todaysBookings   = scalarLong(conn,
                    "SELECT COUNT(*) FROM bookings WHERE booking_date = CURRENT_DATE");
            long todaysCheckIns   = scalarLong(conn,
                    "SELECT COUNT(*) FROM checkins WHERE DATE(checkin_time) = CURRENT_DATE");

            BigDecimal monthlyRevenue = scalarBigDecimal(conn, """
                    SELECT COALESCE(SUM(amount), 0) FROM payments
                    WHERE status = 'PAID'
                      AND payment_date >= DATE_TRUNC('month', CURRENT_DATE)
                    """);
            BigDecimal pendingPayments = scalarBigDecimal(conn, """
                    SELECT COALESCE(SUM(amount), 0) FROM payments
                    WHERE status IN ('PENDING','PARTIALLY_PAID')
                    """);

            double occupancyRate = totalDesksCount == 0 ? 0.0
                    : (occupiedDesksToday * 100.0) / totalDesksCount;

            return new DashboardStats(
                    totalMembers, activeMembers, expiredMembers,
                    availableDesksToday, occupiedDesksToday,
                    availableOfficesToday, occupiedOfficesToday,
                    todaysBookings, todaysCheckIns,
                    monthlyRevenue, pendingPayments,
                    occupancyRate);
        });
    }

    private long scalarLong(Connection conn, String sql) throws SQLException {
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private BigDecimal scalarBigDecimal(Connection conn, String sql) throws SQLException {
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            BigDecimal v = rs.getBigDecimal(1);
            return v == null ? BigDecimal.ZERO : v;
        }
    }
}