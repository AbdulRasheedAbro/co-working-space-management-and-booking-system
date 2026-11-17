package com.time.coworking.controller;

import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.enums.WorkspaceType;
import com.time.coworking.model.Workspace;
import com.time.coworking.service.WorkspaceService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class WorkspaceController {

    private final WorkspaceService service = new WorkspaceService();

    public List<Workspace> list()                            { return service.findAll(); }
    public List<Workspace> byType(WorkspaceType type)         { return service.findByType(type); }
    public List<Workspace> availableByType(WorkspaceType t)   { return service.findAvailableByType(t); }

    /** ⭐ Real availability — free workspaces for a slot. */
    public List<Workspace> availableForSlot(WorkspaceType type, LocalDate date,
                                            LocalTime start, LocalTime end) {
        return service.findAvailableForSlot(type, date, start, end);
    }

    /** ⭐ Real availability — count of free workspaces for a slot. */
    public long countAvailableForSlot(WorkspaceType type, LocalDate date,
                                      LocalTime start, LocalTime end) {
        return service.countAvailableForSlot(type, date, start, end);
    }

    public Workspace get(long id)                             { return service.findById(id); }
    public Workspace add(Workspace w)                         { return service.add(w); }
    public Workspace update(Workspace w)                      { return service.update(w); }
    public void changeStatus(long id, WorkspaceStatus s)      { service.changeStatus(id, s); }
    public void delete(long id)                               { service.delete(id); }
}