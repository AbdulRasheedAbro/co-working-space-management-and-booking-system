package com.time.coworking.repository;

import com.time.coworking.exception.DataAccessException;
import com.time.coworking.model.MembershipPlan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MembershipPlanRepository {

    // ─────────── READ ───────────

    public Optional<MembershipPlan> findById(long id) {
        try (Connection conn = Database.getConnection()) { return findById(conn, id); }
        catch (SQLException e) { throw new DataAccessException("findById plan failed", e); }
    }

    public Optional<MembershipPlan> findById(Connection conn, long id) {
        String sql = "SELECT * FROM membership_plans WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DataAccessException("findById plan failed", e); }
    }

    public List<MembershipPlan> findAll() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM membership_plans ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<MembershipPlan> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DataAccessException("findAll plans failed", e); }
    }

    public List<MembershipPlan> findActive() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM membership_plans WHERE active = TRUE ORDER BY plan_name");
             ResultSet rs = ps.executeQuery()) {
            List<MembershipPlan> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DataAccessException("findActive plans failed", e); }
    }

    // ─────────── WRITE ───────────

    public long insert(Connection conn, MembershipPlan p) {
        String sql = """
                INSERT INTO membership_plans
                    (plan_name, duration_days, price, allowed_hours,
                     included_meeting_hours, description, active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) { p.setId(k.getLong(1)); return p.getId(); }
            }
            return 0L;
        } catch (SQLException e) { throw new DataAccessException("insert plan failed", e); }
    }

    public void update(Connection conn, MembershipPlan p) {
        String sql = """
                UPDATE membership_plans
                   SET plan_name = ?, duration_days = ?, price = ?,
                       allowed_hours = ?, included_meeting_hours = ?,
                       description = ?, active = ?
                 WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, p);
            ps.setLong(8, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("update plan failed", e); }
    }

    public void toggleActive(Connection conn, long id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE membership_plans SET active = NOT active WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("toggleActive plan failed", e); }
    }

    public void delete(Connection conn, long id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM membership_plans WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("delete plan failed", e); }
    }

    // ─────────── MAPPING ───────────

    private void bind(PreparedStatement ps, MembershipPlan p) throws SQLException {
        ps.setString(1, p.getPlanName());
        ps.setInt(2, p.getDurationDays());
        ps.setBigDecimal(3, p.getPrice());
        ps.setInt(4, p.getAllowedHours());
        ps.setInt(5, p.getIncludedMeetingHours());
        ps.setString(6, p.getDescription());
        ps.setBoolean(7, p.isActive());
    }

    private MembershipPlan map(ResultSet rs) throws SQLException {
        return new MembershipPlan(
                rs.getLong("id"),
                rs.getString("plan_name"),
                rs.getInt("duration_days"),
                rs.getBigDecimal("price"),
                rs.getInt("allowed_hours"),
                rs.getInt("included_meeting_hours"),
                rs.getString("description"),
                rs.getBoolean("active"));
    }
}