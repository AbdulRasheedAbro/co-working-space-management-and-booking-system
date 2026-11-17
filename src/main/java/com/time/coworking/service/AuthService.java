package com.time.coworking.service;

import com.time.coworking.Session;
import com.time.coworking.model.User;
import com.time.coworking.repository.AuditLogRepository;
import com.time.coworking.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class AuthService {

    private final UserRepository userRepo = new UserRepository();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    /**
     * @return the authenticated user
     * @throws IllegalArgumentException on bad credentials or disabled account
     */
    public User login(String username, String plainPassword) {
        if (username == null || username.isBlank()
                || plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        Optional<User> opt = userRepo.findByUsername(username.trim());
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User u = opt.get();
        if (!u.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }
        if (!BCrypt.checkpw(plainPassword, u.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        Session.login(u);
        auditRepo.log(Session.getUsername(), "Login", "User", String.valueOf(u.getId()));

        return u;
    }

    public void logout() {
        if (Session.isLoggedIn()) {
            auditRepo.log(Session.getUsername(), "Logout", "User",
                    String.valueOf(Session.getUser().getId()));
        }
        Session.logout();
    }
}