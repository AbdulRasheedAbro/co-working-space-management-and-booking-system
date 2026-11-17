package com.time.coworking.service;

import com.time.coworking.Session;
import com.time.coworking.exception.InvalidMembershipException;
import com.time.coworking.model.MembershipPlan;
import com.time.coworking.repository.AuditLogRepository;
import com.time.coworking.repository.Database;
import com.time.coworking.repository.MembershipPlanRepository;

import java.math.BigDecimal;
import java.util.List;

public class MembershipPlanService {

    private final MembershipPlanRepository repo = new MembershipPlanRepository();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    public List<MembershipPlan> findAll()   { return repo.findAll(); }
    public List<MembershipPlan> findActive() { return repo.findActive(); }

    public MembershipPlan findById(long id) {
        return repo.findById(id).orElseThrow(() ->
                new InvalidMembershipException("Plan #" + id + " not found"));
    }

    public MembershipPlan add(MembershipPlan p) {
        validate(p);

        return Database.inTransaction(conn -> {
            repo.insert(conn, p);
            auditRepo.log(conn, Session.getUsername(), "Created plan",
                    "Plan", p.getPlanName());
            return p;
        });
    }

    public MembershipPlan update(MembershipPlan p) {
        validate(p);

        return Database.inTransaction(conn -> {
            repo.findById(conn, p.getId())
                    .orElseThrow(() -> new InvalidMembershipException(
                            "Plan #" + p.getId() + " not found"));

            repo.update(conn, p);
            auditRepo.log(conn, Session.getUsername(), "Updated plan",
                    "Plan", p.getPlanName());
            return p;
        });
    }

    public void toggleActive(long id) {
        Database.inTransaction(conn -> {
            repo.toggleActive(conn, id);
            auditRepo.log(conn, Session.getUsername(), "Toggled plan active",
                    "Plan", String.valueOf(id));
            return null;
        });
    }

    public void delete(long id) {
        Database.inTransaction(conn -> {
            repo.findById(conn, id).orElseThrow(() ->
                    new InvalidMembershipException("Plan #" + id + " not found"));
            repo.delete(conn, id);
            auditRepo.log(conn, Session.getUsername(), "Deleted plan",
                    "Plan", String.valueOf(id));
            return null;
        });
    }

    // ─────────── Validation ───────────

    private void validate(MembershipPlan p) {
        if (p.getPlanName() == null || p.getPlanName().isBlank()) {
            throw new InvalidMembershipException("Plan name is required");
        }
        if (p.getDurationDays() <= 0) {
            throw new InvalidMembershipException("Duration must be greater than 0 days");
        }
        if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMembershipException("Price must be zero or greater");
        }
        if (p.getAllowedHours() < 0) {
            throw new InvalidMembershipException("Allowed hours must be zero or greater");
        }
        if (p.getIncludedMeetingHours() < 0) {
            throw new InvalidMembershipException("Included meeting hours must be zero or greater");
        }
    }
}