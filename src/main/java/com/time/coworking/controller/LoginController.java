package com.time.coworking.controller;

import com.time.coworking.model.User;
import com.time.coworking.service.AuthService;

public class LoginController {

    private final AuthService authService = new AuthService();

    /**
     * Attempts login.
     * @return the authenticated User on success
     * @throws IllegalArgumentException with a user-facing message on failure
     */
    public User login(String username, String password) {
        return authService.login(username, password);
    }

    public void logout() {
        authService.logout();
    }
}