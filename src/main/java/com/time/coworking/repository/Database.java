package com.time.coworking.repository;

import com.time.coworking.exception.DataAccessException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Connection pool + transaction helper.
 * Task03 §13 requires explicit  setAutoCommit(false) / commit() / rollback().
 * The inTransaction() method below is the single place where that pattern lives.
 */
public final class Database {

    private static final HikariDataSource DS;

    static {
        try {
            Properties p = new Properties();
            try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
                if (in == null) {
                    throw new IllegalStateException(
                            "db.properties not found on classpath (should be in src/main/resources/)");
                }
                p.load(in);
            }

            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(p.getProperty("db.url"));
            cfg.setUsername(p.getProperty("db.user"));
            cfg.setPassword(p.getProperty("db.password"));
            cfg.setMaximumPoolSize(Integer.parseInt(p.getProperty("db.pool.size", "10")));
            cfg.setPoolName("time-coworking-pool");
            cfg.setAutoCommit(true);   // default; inTransaction() overrides per-call

            DS = new HikariDataSource(cfg);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Cannot initialize database pool: " + e.getMessage());
        }
    }

    private Database() {
    }

    /** Borrow a pooled connection (auto-commit = true by default). */
    public static Connection getConnection() throws SQLException {
        return DS.getConnection();
    }

    /**
     * Runs {@code work} inside a database transaction.
     * Commits on success, rolls back on ANY exception.
     * <p>
     * The lambda receives the connection and must use it for all DB calls
     * so they participate in the same transaction.
     */
    public static <T> T inTransaction(TransactionalWork<T> work) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                safeRollback(conn);
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new DataAccessException("Transaction failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Cannot obtain connection: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private static void safeRollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    /** Shut down the pool (call on application exit). */
    public static void shutdown() {
        if (DS != null && !DS.isClosed()) {
            DS.close();
        }
    }

    @FunctionalInterface
    public interface TransactionalWork<T> {
        T execute(Connection conn) throws Exception;
    }
}