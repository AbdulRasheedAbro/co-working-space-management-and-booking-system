package com.time.coworking.repository;

import com.time.coworking.exception.DataAccessException;
import com.time.coworking.model.CheckIn;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CheckInRepository {

    public Optional<CheckIn> findById(long id) {
        try (Connection conn = Database.getConnection()) { return findById(conn, id); }
        catch (SQLException e) { throw new DataAccessException("findById checkin failed", e); }
    }

    public Optional<CheckIn> findById(Connection conn, long id) {
        String sql = "SELECT * FROM checkins WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DataAccessException("findById checkin failed", e); }
    }

    public Optional<CheckIn> findByBooking(Connection conn, long bookingId) {
        String sql = "SELECT * FROM checkins WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DataAccessException("findByBooking failed", e); }
    }

    public Optional<CheckIn> findByBooking(long bookingId) {
        try (Connection conn = Database.getConnection()) { return findByBooking(conn, bookingId); }
        catch (SQLException e) { throw new DataAccessException("findByBooking (auto) failed", e); }
    }

    public List<CheckIn> findAll() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM checkins ORDER BY checkin_time DESC NULLS LAST");
             ResultSet rs = ps.executeQuery()) {
            List<CheckIn> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DataAccessException("findAll checkins failed", e); }
    }

    public List<CheckIn> findToday() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM checkins WHERE DATE(checkin_time) = CURRENT_DATE ORDER BY checkin_time DESC");
             ResultSet rs = ps.executeQuery()) {
            List<CheckIn> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DataAccessException("findToday checkins failed", e); }
    }

    public long insert(Connection conn, CheckIn c) {
        String sql = """
                INSERT INTO checkins
                    (booking_id, checkin_time, checkout_time, actual_min, extra_charge)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, c.getBookingId());
            if (c.getCheckinTime() != null) ps.setTimestamp(2, Timestamp.valueOf(c.getCheckinTime()));
            else ps.setNull(2, Types.TIMESTAMP);
            if (c.getCheckoutTime() != null) ps.setTimestamp(3, Timestamp.valueOf(c.getCheckoutTime()));
            else ps.setNull(3, Types.TIMESTAMP);
            if (c.getActualMin() != null) ps.setInt(4, c.getActualMin());
            else ps.setNull(4, Types.INTEGER);
            ps.setBigDecimal(5, c.getExtraCharge() == null ? BigDecimal.ZERO : c.getExtraCharge());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) { c.setId(k.getLong(1)); return c.getId(); }
            }
            return 0L;
        } catch (SQLException e) { throw new DataAccessException("insert checkin failed", e); }
    }

    public void update(Connection conn, CheckIn c) {
        String sql = """
                UPDATE checkins
                   SET checkin_time = ?, checkout_time = ?,
                       actual_min = ?, extra_charge = ?
                 WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (c.getCheckinTime() != null) ps.setTimestamp(1, Timestamp.valueOf(c.getCheckinTime()));
            else ps.setNull(1, Types.TIMESTAMP);
            if (c.getCheckoutTime() != null) ps.setTimestamp(2, Timestamp.valueOf(c.getCheckoutTime()));
            else ps.setNull(2, Types.TIMESTAMP);
            if (c.getActualMin() != null) ps.setInt(3, c.getActualMin());
            else ps.setNull(3, Types.INTEGER);
            ps.setBigDecimal(4, c.getExtraCharge() == null ? BigDecimal.ZERO : c.getExtraCharge());
            ps.setLong(5, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("update checkin failed", e); }
    }

    private CheckIn map(ResultSet rs) throws SQLException {
        Timestamp in  = rs.getTimestamp("checkin_time");
        Timestamp out = rs.getTimestamp("checkout_time");
        Integer actual = (Integer) rs.getObject("actual_min");
        BigDecimal extra = rs.getBigDecimal("extra_charge");

        CheckIn c = new CheckIn(rs.getLong("id"), rs.getLong("booking_id"));
        c.setCheckinTime(in == null ? null : in.toLocalDateTime());
        c.setCheckoutTime(out == null ? null : out.toLocalDateTime());
        c.setActualMin(actual);
        c.setExtraCharge(extra == null ? BigDecimal.ZERO : extra);
        return c;
    }
}