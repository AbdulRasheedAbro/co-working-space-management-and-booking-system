package com.time.coworking.util;

import java.util.regex.Pattern;

/**
 * Central validation rules (Task03 §19).
 * Used by services before persisting.
 */
public final class Validators {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Allows: +923001234567, 03001234567, 923001234567 */
    private static final Pattern PHONE =
            Pattern.compile("^\\+?[0-9]{10,15}$");

    /** Pakistani CNIC: 12345-1234567-1 or 13 digits. */
    private static final Pattern CNIC =
            Pattern.compile("^(\\d{5}-\\d{7}-\\d|\\d{13})$");

    private Validators() {
    }

    public static boolean isValidEmail(String s) {
        return s != null && EMAIL.matcher(s).matches();
    }

    public static boolean isValidPhone(String s) {
        return s != null && PHONE.matcher(s).matches();
    }

    public static boolean isValidCnic(String s) {
        return s != null && CNIC.matcher(s).matches();
    }

    public static void requireEmail(String s) {
        if (!isValidEmail(s)) throw new IllegalArgumentException("Invalid email: " + s);
    }

    public static void requirePhone(String s) {
        if (!isValidPhone(s)) throw new IllegalArgumentException("Invalid phone: " + s);
    }

    public static void requireCnic(String s) {
        if (!isValidCnic(s)) throw new IllegalArgumentException("Invalid CNIC: " + s);
    }

    public static void requireNotBlank(String s, String field) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException(field + " must not be blank");
    }

    public static void requirePositive(java.math.BigDecimal v, String field) {
        if (v == null || v.signum() < 0)
            throw new IllegalArgumentException(field + " must be >= 0");
    }
}