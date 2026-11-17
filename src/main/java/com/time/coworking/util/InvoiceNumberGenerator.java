package com.time.coworking.util;

import com.time.coworking.repository.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates invoice numbers like INV-2026-0001.
 * Initializes its counter from the DB so restarts don't reset numbering.
 */
public final class InvoiceNumberGenerator {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static int YEAR = Year.now().getValue();
    private static boolean initialized = false;

    private InvoiceNumberGenerator() {}

    public static synchronized String next() {
        if (!initialized) {
            initialize();
        }
        int currentYear = Year.now().getValue();
        if (currentYear != YEAR) {
            YEAR = currentYear;
            COUNTER.set(0);
        }
        int seq = COUNTER.incrementAndGet();
        return String.format("INV-%d-%04d", YEAR, seq);
    }

    private static void initialize() {
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT COUNT(*) FROM invoices WHERE invoice_number LIKE ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "INV-" + YEAR + "-%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        COUNTER.set((int) rs.getLong(1));
                    }
                }
            }
        } catch (Exception e) {
            // Fall back to 0 on failure
        }
        initialized = true;
    }
}