package com.time.coworking.model;

import java.time.LocalDate;
import java.util.Objects;

public class MemberSubscription {

    private long id;
    private long memberId;
    private long planId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;

    public MemberSubscription() {
    }

    public MemberSubscription(long id, long memberId, long planId,
                              LocalDate startDate, LocalDate endDate, boolean active) {
        this.id = id;
        this.memberId = memberId;
        this.planId = planId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMemberId() { return memberId; }
    public void setMemberId(long memberId) { this.memberId = memberId; }

    public long getPlanId() { return planId; }
    public void setPlanId(long planId) { this.planId = planId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberSubscription that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}