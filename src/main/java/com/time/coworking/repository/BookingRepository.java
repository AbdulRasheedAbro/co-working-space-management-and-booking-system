package com.time.coworking.repository;

import com.time.coworking.enums.BookingStatus;
import com.time.coworking.exception.DataAccessException;
import com.time.coworking.model.Booking;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookingRepository {

    // ─────────── READ ───────────

    public Optional<Booking> findById(long id) {
        try (Connection conn = Database.getConnection()) { return findById(conn, id); }
        catch (SQLException e) { throw new DataAccessException("findById booking failed", e); }
    }

    public Optional<Booking> findById(Connection conn, long id) {
        String sql = "SELECT * FROM bookings WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DataAccessException("findById booking failed", e); }
    }

    public List<Booking> findAll() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM bookings ORDER BY booking_date DESC, start_time DESC");
             ResultSet rs = ps.executeQuery()) {
            List<Booking> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DataAccessException("findAll bookings failed", e); }
    }

    public List<Booking> findToday() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM bookings WHERE booking_date <= CURRENT_DATE AND end_date >= CURRENT_DATE ORDER BY start_time");
             ResultSet rs = ps.executeQuery()) {
            List<Booking> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DataAccessException("findToday failed", e); }
    }

    public List<Booking> search(LocalDate from, LocalDate to, Long memberId,
                                Long workspaceId, BookingStatus status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM bookings WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (from != null)     { sql.append(" AND end_date >= ?");     params.add(Date.valueOf(from)); }
        if (to != null)       { sql.append(" AND booking_date <= ?"); params.add(Date.valueOf(to)); }
        if (memberId != null) { sql.append(" AND member_id = ?");     params.add(memberId); }
        if (workspaceId != null) { sql.append(" AND workspace_id = ?"); params.add(workspaceId); }
        if (status != null)   { sql.append(" AND status = ?");        params.add(status.name()); }

        sql.append(" ORDER BY booking_date DESC, start_time DESC");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<Booking> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DataAccessException("search bookings failed", e); }
    }

    // ─────────── CONFLICT DETECTION ───────────

    /**
     * Single-day conflict check — used for single-day bookings.
     * Overlap rule: newStart < existingEnd AND newEnd > existingStart.
     */
    public long countOverlapping(Connection conn, long workspaceId, LocalDate date,
                                 LocalTime startTime, LocalTime endTime) {
        String sql = """
                SELECT COUNT(*) FROM bookings
                WHERE workspace_id = ?
                  AND booking_date <= ?
                  AND end_date     >= ?
                  AND status IN ('PENDING','CONFIRMED','CHECKED_IN')
                  AND start_time < ?
                  AND end_time   > ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workspaceId);
            ps.setObject(2, date);
            ps.setObject(3, date);
            ps.setObject(4, endTime);
            ps.setObject(5, startTime);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("countOverlapping failed", e);
        }
    }

    /**
     * Range conflict check — any existing booking that overlaps the requested
     * date range AND time range.
     *
     * Two ranges overlap if:
     *   existing.booking_date <= new.end_date  AND  existing.end_date >= new.booking_date
     */
    public long countOverlappingRange(Connection conn, long workspaceId,
                                      LocalDate startDate, LocalDate endDate,
                                      LocalTime startTime, LocalTime endTime) {
        String sql = """
                SELECT COUNT(*) FROM bookings
                WHERE workspace_id = ?
                  AND booking_date <= ?
                  AND end_date     >= ?
                  AND status IN ('PENDING','CONFIRMED','CHECKED_IN')
                  AND start_time < ?
                  AND end_time   > ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workspaceId);
            ps.setObject(2, endDate);
            ps.setObject(3, startDate);
            ps.setObject(4, endTime);
            ps.setObject(5, startTime);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("countOverlappingRange failed", e);
        }
    }

    // ─────────── WRITE ───────────

    public long insert(Connection conn, Booking b) {
        String sql = """
                INSERT INTO bookings
                    (booking_code, member_id, workspace_id, booking_date, end_date,
                     start_time, end_time, duration_min, status, amount)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getBookingCode());
            ps.setLong(2, b.getMemberId());
            ps.setLong(3, b.getWorkspaceId());
            ps.setDate(4, Date.valueOf(b.getBookingDate()));
            ps.setDate(5, Date.valueOf(b.getEndDate()));
            ps.setTime(6, Time.valueOf(b.getStartTime()));
            ps.setTime(7, Time.valueOf(b.getEndTime()));
            ps.setInt(8, b.getDurationMin());
            ps.setString(9, b.getStatus().name());
            ps.setBigDecimal(10, b.getAmount());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) { b.setId(k.getLong(1)); return b.getId(); }
            }
            return 0L;
        } catch (SQLException e) { throw new DataAccessException("insert booking failed", e); }
    }

    public void updateStatus(Connection conn, long id, BookingStatus status) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE bookings SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("updateStatus booking failed", e); }
    }

    // ─────────── MAPPING ───────────

    private Booking map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return new Booking(
                rs.getLong("id"),
                rs.getString("booking_code"),
                rs.getLong("member_id"),
                rs.getLong("workspace_id"),
                rs.getDate("booking_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                rs.getInt("duration_min"),
                BookingStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount"),
                created == null ? LocalDateTime.now() : created.toLocalDateTime());
    }
}