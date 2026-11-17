package com.time.coworking.repository;

import com.time.coworking.enums.Role;
import com.time.coworking.exception.DataAccessException;
import com.time.coworking.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public class UserRepository {

    public Optional<User> findByUsername(Connection conn, String username) {
        String sql = """
                SELECT id, username, password, full_name, role, enabled, created_at
                FROM users
                WHERE username = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("findByUsername failed", e);
        }
    }

    /** Convenience overload that opens its own connection (auto-commit). */
    public Optional<User> findByUsername(String username) {
        try (Connection conn = Database.getConnection()) {
            return findByUsername(conn, username);
        } catch (SQLException e) {
            throw new DataAccessException("findByUsername (auto) failed", e);
        }
    }

    public long count(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DataAccessException("count users failed", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"),
                Role.valueOf(rs.getString("role")),
                rs.getBoolean("enabled"),
                created == null ? null : created.toLocalDateTime()
        );
    }
}