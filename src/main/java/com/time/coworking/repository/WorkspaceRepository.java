package com.time.coworking.repository;

import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.enums.WorkspaceType;
import com.time.coworking.exception.DataAccessException;
import com.time.coworking.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkspaceRepository {

    // ─────────── READ ───────────

    public Optional<Workspace> findById(long id) {
        try (Connection conn = Database.getConnection()) { return findById(conn, id); }
        catch (SQLException e) { throw new DataAccessException("findById workspace failed", e); }
    }

    public Optional<Workspace> findById(Connection conn, long id) {
        String sql = "SELECT * FROM workspaces WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Workspace w = map(rs);
                    w.setStatus(computeEffectiveStatus(w));
                    return Optional.of(w);
                }
                return Optional.empty();
            }
        } catch (SQLException e) { throw new DataAccessException("findById workspace failed", e); }
    }

    public List<Workspace> findAll() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM workspaces ORDER BY type, code");
             ResultSet rs = ps.executeQuery()) {
            List<Workspace> out = new ArrayList<>();
            while (rs.next()) {
                Workspace w = map(rs);
                w.setStatus(computeEffectiveStatus(w));
                out.add(w);
            }
            return out;
        } catch (SQLException e) { throw new DataAccessException("findAll workspaces failed", e); }
    }

    public List<Workspace> findByType(WorkspaceType type) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM workspaces WHERE type = ? ORDER BY code")) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<Workspace> out = new ArrayList<>();
                while (rs.next()) {
                    Workspace w = map(rs);
                    w.setStatus(computeEffectiveStatus(w));
                    out.add(w);
                }
                return out;
            }
        } catch (SQLException e) { throw new DataAccessException("findByType failed", e); }
    }

    public List<Workspace> findAvailableByType(WorkspaceType type) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM workspaces WHERE type = ? AND status = 'AVAILABLE' ORDER BY code")) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<Workspace> out = new ArrayList<>();
                while (rs.next()) {
                    Workspace w = map(rs);
                    w.setStatus(computeEffectiveStatus(w));
                    out.add(w);
                }
                return out;
            }
        } catch (SQLException e) { throw new DataAccessException("findAvailableByType failed", e); }
    }

    public List<Workspace> findAvailableForSlot(WorkspaceType type, LocalDate date,
                                                LocalTime start, LocalTime end) {
        String sql = """
                SELECT * FROM workspaces w
                WHERE w.type = ?
                  AND w.status NOT IN ('MAINTENANCE','UNAVAILABLE')
                  AND NOT EXISTS (
                      SELECT 1 FROM bookings b
                      WHERE b.workspace_id = w.id
                        AND b.booking_date = ?
                        AND b.status IN ('PENDING','CONFIRMED','CHECKED_IN')
                        AND b.start_time < ?
                        AND b.end_time   > ?
                  )
                ORDER BY w.code
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ps.setObject(2, date);
            ps.setObject(3, end);
            ps.setObject(4, start);
            try (ResultSet rs = ps.executeQuery()) {
                List<Workspace> out = new ArrayList<>();
                while (rs.next()) {
                    Workspace w = map(rs);
                    w.setStatus(WorkspaceStatus.AVAILABLE);
                    out.add(w);
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("findAvailableForSlot failed", e);
        }
    }

    public long countAvailableForSlot(WorkspaceType type, LocalDate date,
                                      LocalTime start, LocalTime end) {
        String sql = """
                SELECT COUNT(*) FROM workspaces w
                WHERE w.type = ?
                  AND w.status NOT IN ('MAINTENANCE','UNAVAILABLE')
                  AND NOT EXISTS (
                      SELECT 1 FROM bookings b
                      WHERE b.workspace_id = w.id
                        AND b.booking_date = ?
                        AND b.status IN ('PENDING','CONFIRMED','CHECKED_IN')
                        AND b.start_time < ?
                        AND b.end_time   > ?
                  )
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ps.setObject(2, date);
            ps.setObject(3, end);
            ps.setObject(4, start);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("countAvailableForSlot failed", e);
        }
    }

    // ─────────── WRITE ───────────

    public long insert(Connection conn, Workspace w) {
        String sql = """
                INSERT INTO workspaces
                    (type, code, floor, zone, capacity, hourly_price,
                     monthly_price, facilities, status,
                     assigned_member_id, assign_start, assign_end)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, w);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) { w.setId(k.getLong(1)); return w.getId(); }
            }
            return 0L;
        } catch (SQLException e) { throw new DataAccessException("insert workspace failed", e); }
    }

    public void update(Connection conn, Workspace w) {
        String sql = """
                UPDATE workspaces
                   SET type = ?, code = ?, floor = ?, zone = ?, capacity = ?,
                       hourly_price = ?, monthly_price = ?, facilities = ?,
                       status = ?, assigned_member_id = ?, assign_start = ?, assign_end = ?
                 WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, w);
            ps.setLong(13, w.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("update workspace failed", e); }
    }

    public void updateStatus(Connection conn, long id, WorkspaceStatus status) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE workspaces SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("updateStatus failed", e); }
    }

    public void delete(Connection conn, long id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM workspaces WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("delete workspace failed", e); }
    }

    // ─────────── EFFECTIVE STATUS ───────────

    /**
     * Computes the status the user sees and fills reservedBy / reservedSlot.
     */
    private WorkspaceStatus computeEffectiveStatus(Workspace w) {
        WorkspaceStatus stored = w.getStatus();

        if (stored == WorkspaceStatus.MAINTENANCE
                || stored == WorkspaceStatus.UNAVAILABLE
                || stored == WorkspaceStatus.OCCUPIED) {
            return stored;
        }

        // ⭐ THE FIX IS HERE ⭐
        // Changed from booking_date >= CURRENT_DATE to an overlap check:
        //   booking started today or earlier AND ends today or later
        String sql = """
                SELECT
                    m.full_name                AS member_name,
                    b.booking_date             AS first_date,
                    b.end_date                 AS last_date,
                    b.start_time               AS first_start,
                    b.end_time                 AS last_end
                FROM bookings b
                JOIN members m ON m.id = b.member_id
                WHERE b.workspace_id = ?
                  AND b.booking_date <= CURRENT_DATE
                  AND b.end_date     >= CURRENT_DATE
                  AND b.status IN ('PENDING','CONFIRMED','CHECKED_IN')
                ORDER BY b.booking_date ASC, b.start_time ASC
                LIMIT 1
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, w.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    w.setReservedBy(null);
                    w.setReservedSlot(null);
                    return WorkspaceStatus.AVAILABLE;
                }

                String memberName = rs.getString("member_name");
                LocalDate firstDate = rs.getDate("first_date").toLocalDate();
                LocalDate lastDate  = rs.getDate("last_date").toLocalDate();
                LocalTime firstStart = rs.getTime("first_start").toLocalTime();
                LocalTime lastEnd    = rs.getTime("last_end").toLocalTime();

                w.setReservedBy(memberName);

                if (firstDate.equals(lastDate)) {
                    w.setReservedSlot(String.format("%s  %s–%s",
                            firstDate, firstStart, lastEnd));
                } else {
                    long days = ChronoUnit.DAYS.between(firstDate, lastDate) + 1;
                    w.setReservedSlot(String.format("%s %s → %s %s  (%d day%s)",
                            firstDate, firstStart,
                            lastDate, lastEnd,
                            days, days == 1 ? "" : "s"));
                }

                return WorkspaceStatus.RESERVED;
            }
        } catch (SQLException e) {
            throw new DataAccessException("computeEffectiveStatus failed", e);
        }
    }

    // ─────────── MAPPING ───────────

    private void bind(PreparedStatement ps, Workspace w) throws SQLException {
        ps.setString(1, w.getType().name());
        ps.setString(2, w.getCode());
        ps.setString(3, w.getFloor());
        ps.setString(4, w.getZone());
        if (w.getCapacity() > 0) ps.setInt(5, w.getCapacity());
        else ps.setNull(5, Types.INTEGER);

        if (w instanceof MeetingRoom mr) {
            ps.setBigDecimal(6, mr.getHourlyPrice());
            ps.setNull(7, Types.NUMERIC);
        } else {
            ps.setNull(6, Types.NUMERIC);
            ps.setBigDecimal(7, w.getPrice());
        }
        ps.setString(8, w.getFacilities());
        ps.setString(9, w.getStatus().name());
        if (w.getAssignedMemberId() != null) ps.setLong(10, w.getAssignedMemberId());
        else ps.setNull(10, Types.BIGINT);
        if (w.getAssignStart() != null) ps.setDate(11, Date.valueOf(w.getAssignStart()));
        else ps.setNull(11, Types.DATE);
        if (w.getAssignEnd() != null) ps.setDate(12, Date.valueOf(w.getAssignEnd()));
        else ps.setNull(12, Types.DATE);
    }

    private Workspace map(ResultSet rs) throws SQLException {
        String typeStr = rs.getString("type");
        WorkspaceType type = WorkspaceType.valueOf(typeStr);

        long id = rs.getLong("id");
        String code = rs.getString("code");
        String floor = rs.getString("floor");
        String zone = rs.getString("zone");
        int capacity = rs.getInt("capacity");
        WorkspaceStatus status = WorkspaceStatus.valueOf(rs.getString("status"));
        Long assignedMemberId = (Long) rs.getObject("assigned_member_id");
        Date as = rs.getDate("assign_start");
        Date ae = rs.getDate("assign_end");
        LocalDate assignStart = as == null ? null : as.toLocalDate();
        LocalDate assignEnd = ae == null ? null : ae.toLocalDate();
        String facilities = rs.getString("facilities");
        BigDecimal hourly = rs.getBigDecimal("hourly_price");
        BigDecimal monthly = rs.getBigDecimal("monthly_price");

        switch (type) {
            case HOT_DESK:
            case DEDICATED_DESK:
                return new Desk(id, code, floor, zone, status,
                        assignedMemberId, assignStart, assignEnd,
                        type, monthly, facilities);
            case OFFICE:
                return new Office(id, code, floor, zone, capacity, status,
                        assignedMemberId, assignStart, assignEnd, monthly, facilities);
            case MEETING_ROOM:
                return new MeetingRoom(id, code, floor, zone, capacity,
                        status, hourly, facilities);
            default:
                throw new DataAccessException("Unknown workspace type: " + typeStr, null);
        }
    }
}