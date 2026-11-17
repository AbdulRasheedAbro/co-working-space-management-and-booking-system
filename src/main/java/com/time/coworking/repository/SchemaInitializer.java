package com.time.coworking.repository;

import com.time.coworking.exception.DataAccessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensures the database schema exists at application startup.
 *
 * <p>If the {@code users} table is missing, the whole {@code schema.sql}
 * script is executed. Then, if the {@code membership_plans} table is empty,
 * {@code seed.sql} is executed to insert demo data.
 *
 * <p>The SQL files themselves are the authoritative source of truth for the
 * schema — this class just applies them idempotently on first run.
 */
public final class SchemaInitializer {

    private static final String SCHEMA_SQL = "/db/schema.sql";
    private static final String SEED_SQL   = "/db/seed.sql";

    private SchemaInitializer() {
    }

    /** Run this once at application startup (from Main/Launcher). */
    public static void initialize() {
        try (Connection conn = Database.getConnection()) {

            if (!tableExists(conn, "users")) {
                System.out.println("[Schema] 'users' table not found — running schema.sql");
                runSqlScript(conn, SCHEMA_SQL);
                System.out.println("[Schema] schema.sql applied.");
            } else {
                System.out.println("[Schema] Schema already present — skipping schema.sql");
            }

            if (tableIsEmpty(conn, "membership_plans")) {
                System.out.println("[Schema] 'membership_plans' is empty — running seed.sql");
                runSqlScript(conn, SEED_SQL);
                System.out.println("[Schema] seed.sql applied.");
            } else {
                System.out.println("[Schema] Seed data already present — skipping seed.sql");
            }

        } catch (Exception e) {
            throw new DataAccessException(
                    "Database initialization failed: " + e.getMessage(), e);
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────────────────────

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = """
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean tableIsEmpty(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT 1 FROM " + tableName + " LIMIT 1";
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return !rs.next();
        }
    }

    /**
     * Reads a SQL script from the classpath, splits it on semicolons that end
     * statements, and executes each statement in order.
     *
     * <p>Note: this naive splitter handles the scripts we control. It does
     * NOT handle semicolons inside string literals, comments, or PL/pgSQL
     * functions. If you later add those, switch to a proper SQL parser or
     * use a migration tool like Flyway.
     */
    private static void runSqlScript(Connection conn, String resourcePath) throws IOException, SQLException {
        String script = readResource(resourcePath);
        List<String> statements = splitStatements(script);

        boolean originalAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                if (sql.isBlank()) continue;
                stmt.execute(sql);
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Resource not found on classpath: " + path);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        }
    }

    /**
     * Splits SQL text into individual statements at top-level semicolons.
     * Strips line comments (--).
     */
    private static List<String> splitStatements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);

            // Handle line comments
            if (!inString && c == '-' && i + 1 < script.length()
                    && script.charAt(i + 1) == '-') {
                // Skip to end of line
                while (i < script.length() && script.charAt(i) != '\n') i++;
                continue;
            }

            // Toggle string literal mode
            if (!inString && (c == '\'' || c == '"')) {
                inString = true;
                stringChar = c;
            } else if (inString && c == stringChar) {
                // Handle escaped quote '' or ""
                if (i + 1 < script.length() && script.charAt(i + 1) == stringChar) {
                    current.append(c).append(c);
                    i++;
                    continue;
                }
                inString = false;
            }

            if (!inString && c == ';') {
                String s = current.toString().trim();
                if (!s.isBlank()) out.add(s);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        String tail = current.toString().trim();
        if (!tail.isBlank()) out.add(tail);

        return out;
    }
}