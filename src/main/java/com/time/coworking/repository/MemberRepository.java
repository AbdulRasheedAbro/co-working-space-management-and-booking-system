package com.time.coworking.repository;

import com.time.coworking.enums.MemberStatus;
import com.time.coworking.exception.DataAccessException;
import com.time.coworking.model.Member;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberRepository {

    // ────────────────────────────── READ ──────────────────────────────

    public Optional<Member> findById(Connection conn, long id) {
        String sql = """
                SELECT id, full_name, email, phone, cnic, company,
                       membership_type, join_date, expiry_date, status
                FROM members
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("findById failed", e);
        }
    }

    public Optional<Member> findByEmail(Connection conn, String email) {
        String sql = """
                SELECT id, full_name, email, phone, cnic, company,
                       membership_type, join_date, expiry_date, status
                FROM members
                WHERE email = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("findByEmail failed", e);
        }
    }

    public List<Member> findAll() {
        String sql = """
                SELECT id, full_name, email, phone, cnic, company,
                       membership_type, join_date, expiry_date, status
                FROM members
                ORDER BY id
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Member> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("findAll members failed", e);
        }
    }

    /** Search by name / email / phone / company / status (all optional). */
    public List<Member> search(String q, MemberStatus status) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, full_name, email, phone, cnic, company,
                       membership_type, join_date, expiry_date, status
                FROM members
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            sql.append("""
                     AND (LOWER(full_name) LIKE ?
                       OR LOWER(email)     LIKE ?
                       OR phone            LIKE ?
                       OR LOWER(COALESCE(company,'')) LIKE ?)
                    """);
            String like = "%" + q.toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add("%" + q + "%");
            params.add(like);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }
        sql.append(" ORDER BY id");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Member> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("search members failed", e);
        }
    }

    // ────────────────────────────── WRITE ─────────────────────────────

    /** Inserts and returns the generated id (0 if unavailable). */
    public long insert(Connection conn, Member m) {
        String sql = """
                INSERT INTO members
                    (full_name, email, phone, cnic, company,
                     membership_type, join_date, expiry_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getFullName());
            ps.setString(2, m.getEmail());
            ps.setString(3, m.getPhone());
            ps.setString(4, m.getCnic());
            ps.setString(5, m.getCompany());
            ps.setString(6, m.getMembershipType());
            ps.setDate(7, Date.valueOf(m.getJoinDate()));
            ps.setDate(8, Date.valueOf(m.getExpiryDate()));
            ps.setString(9, m.getStatus().name());

            int rows = ps.executeUpdate();
            if (rows == 0) throw new DataAccessException("Insert member returned 0 rows", null);

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    m.setId(id);
                    return id;
                }
            }
            return 0L;
        } catch (SQLException e) {
            throw new DataAccessException("insert member failed", e);
        }
    }

    public void update(Connection conn, Member m) {
        String sql = """
                UPDATE members
                   SET full_name = ?, email = ?, phone = ?, cnic = ?,
                       company = ?, membership_type = ?, join_date = ?,
                       expiry_date = ?, status = ?
                 WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getFullName());
            ps.setString(2, m.getEmail());
            ps.setString(3, m.getPhone());
            ps.setString(4, m.getCnic());
            ps.setString(5, m.getCompany());
            ps.setString(6, m.getMembershipType());
            ps.setDate(7, Date.valueOf(m.getJoinDate()));
            ps.setDate(8, Date.valueOf(m.getExpiryDate()));
            ps.setString(9, m.getStatus().name());
            ps.setLong(10, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("update member failed", e);
        }
    }

    public void delete(Connection conn, long id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM members WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("delete member failed", e);
        }
    }

    // ────────────────────────────── MAPPING ───────────────────────────

    private Member map(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setId(rs.getLong("id"));
        m.setFullName(rs.getString("full_name"));
        m.setEmail(rs.getString("email"));
        m.setPhone(rs.getString("phone"));
        m.setCnic(rs.getString("cnic"));
        m.setCompany(rs.getString("company"));
        m.setMembershipType(rs.getString("membership_type"));
        Date jd = rs.getDate("join_date");
        Date ed = rs.getDate("expiry_date");
        m.setJoinDate(jd == null ? null : jd.toLocalDate());
        m.setExpiryDate(ed == null ? null : ed.toLocalDate());
        m.setStatus(MemberStatus.valueOf(rs.getString("status")));
        return m;
    }
}