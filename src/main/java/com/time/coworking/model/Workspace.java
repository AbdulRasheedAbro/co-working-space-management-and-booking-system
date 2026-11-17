package com.time.coworking.model;

import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.enums.WorkspaceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Abstract base for all bookable resources.
 * Demonstrates inheritance + polymorphism (Task03 §15).
 */
public abstract class Workspace {

    private long id;
    private String code;
    private String floor;
    private String zone;
    private int capacity;
    private WorkspaceStatus status;
    private Long assignedMemberId;
    private LocalDate assignStart;
    private LocalDate assignEnd;
    private String facilities;
    private String reservedBy;      // member full name — null if free
    private String reservedSlot;    // "2026-10-28 10:00–12:00" — null if free

    protected Workspace() {
    }

    protected Workspace(long id, String code, String floor, String zone,
                        int capacity, WorkspaceStatus status,
                        Long assignedMemberId, LocalDate assignStart, LocalDate assignEnd) {
        this.id = id;
        this.code = code;
        this.floor = floor;
        this.zone = zone;
        this.capacity = capacity;
        this.status = status;
        this.assignedMemberId = assignedMemberId;
        this.assignStart = assignStart;
        this.assignEnd = assignEnd;
    }

    public abstract WorkspaceType getType();
    public abstract BigDecimal getPrice();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public WorkspaceStatus getStatus() { return status; }
    public void setStatus(WorkspaceStatus status) { this.status = status; }

    public Long getAssignedMemberId() { return assignedMemberId; }
    public void setAssignedMemberId(Long assignedMemberId) { this.assignedMemberId = assignedMemberId; }

    public LocalDate getAssignStart() { return assignStart; }
    public void setAssignStart(LocalDate assignStart) { this.assignStart = assignStart; }

    public LocalDate getAssignEnd() { return assignEnd; }
    public void setAssignEnd(LocalDate assignEnd) { this.assignEnd = assignEnd; }

    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }

    public String getReservedBy() { return reservedBy; }
    public void setReservedBy(String reservedBy) { this.reservedBy = reservedBy; }

    public String getReservedSlot() { return reservedSlot; }
    public void setReservedSlot(String reservedSlot) { this.reservedSlot = reservedSlot; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Workspace that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return code + " (" + getType() + ")"; }
}