package com.time.coworking.service;

import com.time.coworking.Session;
import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.enums.WorkspaceType;
import com.time.coworking.exception.WorkspaceUnavailableException;
import com.time.coworking.model.Workspace;
import com.time.coworking.repository.AuditLogRepository;
import com.time.coworking.repository.Database;
import com.time.coworking.repository.WorkspaceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class WorkspaceService {

    private final WorkspaceRepository repo = new WorkspaceRepository();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    public List<Workspace> findAll()                            { return repo.findAll(); }
    public List<Workspace> findByType(WorkspaceType t)           { return repo.findByType(t); }
    public List<Workspace> findAvailableByType(WorkspaceType t)  { return repo.findAvailableByType(t); }

    /** ⭐ Real availability — list of free workspaces for a slot. */
    public List<Workspace> findAvailableForSlot(WorkspaceType type, LocalDate date,
                                                LocalTime start, LocalTime end) {
        validateSlot(date, start, end);
        return repo.findAvailableForSlot(type, date, start, end);
    }

    /** ⭐ Real availability — count of free workspaces for a slot. */
    public long countAvailableForSlot(WorkspaceType type, LocalDate date,
                                      LocalTime start, LocalTime end) {
        validateSlot(date, start, end);
        return repo.countAvailableForSlot(type, date, start, end);
    }

    public Workspace findById(long id) {
        return repo.findById(id).orElseThrow(() ->
                new WorkspaceUnavailableException("Workspace #" + id + " not found"));
    }

    public Workspace add(Workspace w) {
        validate(w);
        return Database.inTransaction(conn -> {
            repo.insert(conn, w);
            auditRepo.log(conn, Session.getUsername(), "Created workspace",
                    "Workspace", w.getCode());
            return w;
        });
    }

    public Workspace update(Workspace w) {
        validate(w);
        return Database.inTransaction(conn -> {
            repo.update(conn, w);
            auditRepo.log(conn, Session.getUsername(), "Updated workspace",
                    "Workspace", w.getCode());
            return w;
        });
    }

    public void changeStatus(long id, WorkspaceStatus status) {
        Database.inTransaction(conn -> {
            repo.updateStatus(conn, id, status);
            auditRepo.log(conn, Session.getUsername(),
                    "Workspace status → " + status,
                    "Workspace", String.valueOf(id));
            return null;
        });
    }

    public void delete(long id) {
        Database.inTransaction(conn -> {
            repo.delete(conn, id);
            auditRepo.log(conn, Session.getUsername(), "Deleted workspace",
                    "Workspace", String.valueOf(id));
            return null;
        });
    }

    // ─── Validation ───

    private void validateSlot(LocalDate date, LocalTime start, LocalTime end) {
        if (date == null || start == null || end == null) {
            throw new WorkspaceUnavailableException("date, start, end are required");
        }
        if (!end.isAfter(start)) {
            throw new WorkspaceUnavailableException("End time must be after start time");
        }
    }

    private void validate(Workspace w) {
        if (w.getCode() == null || w.getCode().isBlank())
            throw new WorkspaceUnavailableException("Workspace code is required");
        if (w.getStatus() == null)
            w.setStatus(WorkspaceStatus.AVAILABLE);

        BigDecimal price = w.getPrice();
        if (price == null || price.signum() < 0)
            throw new WorkspaceUnavailableException("Price must be >= 0");

        if ((w.getType() == WorkspaceType.MEETING_ROOM
                || w.getType() == WorkspaceType.OFFICE)
                && w.getCapacity() <= 0) {
            throw new WorkspaceUnavailableException(
                    w.getType() + " capacity must be positive");
        }
    }
}