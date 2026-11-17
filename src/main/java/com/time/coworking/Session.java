package com.time.coworking;

import com.time.coworking.model.User;

/**
 * Holds the currently logged-in user for the application lifetime.
 * Set on successful login, cleared on logout.
 * Not thread-safe — always set/read from the JavaFX Application Thread.
 */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static void login(User user) {
        if (user == null) throw new IllegalArgumentException("user must not be null");
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null
                && currentUser.getRole() == com.time.coworking.enums.Role.ADMIN;
    }

    public static String getUsername() {
        return currentUser == null ? "guest" : currentUser.getUsername();
    }
}