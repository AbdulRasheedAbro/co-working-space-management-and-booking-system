package com.time.coworking.model;

import java.math.BigDecimal;
import java.util.Objects;

public class MembershipPlan {

    private long id;
    private String planName;
    private int durationDays;
    private BigDecimal price;
    private int allowedHours;
    private int includedMeetingHours;
    private String description;
    private boolean active;

    public MembershipPlan() {
    }

    public MembershipPlan(long id, String planName, int durationDays, BigDecimal price,
                          int allowedHours, int includedMeetingHours,
                          String description, boolean active) {
        this.id = id;
        this.planName = planName;
        this.durationDays = durationDays;
        this.price = price;
        this.allowedHours = allowedHours;
        this.includedMeetingHours = includedMeetingHours;
        this.description = description;
        this.active = active;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getAllowedHours() { return allowedHours; }
    public void setAllowedHours(int allowedHours) { this.allowedHours = allowedHours; }

    public int getIncludedMeetingHours() { return includedMeetingHours; }
    public void setIncludedMeetingHours(int includedMeetingHours) { this.includedMeetingHours = includedMeetingHours; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipPlan plan)) return false;
        return id == plan.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return planName; }   // useful in ComboBox
}