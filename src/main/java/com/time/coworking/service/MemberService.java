package com.time.coworking.service;

import com.time.coworking.Session;
import com.time.coworking.enums.MemberStatus;
import com.time.coworking.exception.InvalidMembershipException;
import com.time.coworking.exception.MemberNotFoundException;
import com.time.coworking.model.Member;
import com.time.coworking.repository.AuditLogRepository;
import com.time.coworking.repository.Database;
import com.time.coworking.repository.MemberRepository;
import com.time.coworking.util.Validators;

import java.time.LocalDate;
import java.util.List;

public class MemberService {

    private final MemberRepository memberRepo = new MemberRepository();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    public List<Member> findAll() {
        return memberRepo.findAll();
    }

    public List<Member> search(String q, MemberStatus status) {
        return memberRepo.search(q, status);
    }

    public Member findById(long id) {
        return Database.inTransaction(conn ->
                memberRepo.findById(conn, id)
                        .orElseThrow(() -> new MemberNotFoundException("Member #" + id + " not found"))
        );
    }

    /** Add new member with validation. Runs in a transaction. */
    public Member add(Member m) {
        validate(m, true);

        return Database.inTransaction(conn -> {
            // duplicate email check
            if (memberRepo.findByEmail(conn, m.getEmail()).isPresent()) {
                throw new InvalidMembershipException(
                        "A member with email " + m.getEmail() + " already exists");
            }

            memberRepo.insert(conn, m);

            auditRepo.log(conn, Session.getUsername(), "Created member",
                    "Member", String.valueOf(m.getId()));
            return m;
        });
    }

    public Member update(Member m) {
        validate(m, false);

        return Database.inTransaction(conn -> {
            memberRepo.findById(conn, m.getId())
                    .orElseThrow(() -> new MemberNotFoundException("Member #" + m.getId() + " not found"));

            // If the email is being changed, ensure it isn't taken by another member
            memberRepo.findByEmail(conn, m.getEmail()).ifPresent(existing -> {
                if (existing.getId() != m.getId()) {
                    throw new InvalidMembershipException(
                            "Email " + m.getEmail() + " is already used by another member");
                }
            });

            memberRepo.update(conn, m);

            auditRepo.log(conn, Session.getUsername(), "Updated member",
                    "Member", String.valueOf(m.getId()));
            return m;
        });
    }

    public void delete(long id) {
        Database.inTransaction(conn -> {
            Member m = memberRepo.findById(conn, id)
                    .orElseThrow(() -> new MemberNotFoundException("Member #" + id + " not found"));
            memberRepo.delete(conn, id);
            auditRepo.log(conn, Session.getUsername(), "Deleted member",
                    "Member", String.valueOf(m.getId()));
            return null;
        });
    }

    /** Renew a membership: extends expiry by the plan duration. */
    public Member renew(long memberId, int extendDays) {
        if (extendDays <= 0) {
            throw new InvalidMembershipException("Renewal duration must be positive");
        }
        return Database.inTransaction(conn -> {
            Member m = memberRepo.findById(conn, memberId)
                    .orElseThrow(() -> new MemberNotFoundException("Member #" + memberId + " not found"));

            LocalDate base = m.getExpiryDate() != null && m.getExpiryDate().isAfter(LocalDate.now())
                    ? m.getExpiryDate()
                    : LocalDate.now();

            m.setExpiryDate(base.plusDays(extendDays));
            m.setStatus(MemberStatus.ACTIVE);
            m.setJoinDate(m.getJoinDate() == null ? LocalDate.now() : m.getJoinDate());

            memberRepo.update(conn, m);

            auditRepo.log(conn, Session.getUsername(), "Renewed membership",
                    "Member", String.valueOf(m.getId()));
            return m;
        });
    }

    // ─────────────────────── validation rules ──────────────────────────

    private void validate(Member m, boolean isNew) {
        Validators.requireNotBlank(m.getFullName(), "Full name");
        Validators.requireEmail(m.getEmail());
        Validators.requirePhone(m.getPhone());
        Validators.requireCnic(m.getCnic());

        if (m.getJoinDate() == null)
            throw new InvalidMembershipException("Join date is required");
        if (m.getExpiryDate() == null)
            throw new InvalidMembershipException("Expiry date is required");
        if (m.getExpiryDate().isBefore(m.getJoinDate()))
            throw new InvalidMembershipException("Expiry date must be on or after join date");
        if (m.getStatus() == null)
            m.setStatus(MemberStatus.ACTIVE);

        // Optional default on create
        if (isNew && m.getMembershipType() == null) {
            m.setMembershipType("Monthly Hot Desk");
        }
    }
}