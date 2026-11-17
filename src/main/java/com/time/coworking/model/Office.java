package com.time.coworking.model;

import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.enums.WorkspaceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Office extends Workspace {

    private BigDecimal monthlyPrice;

    public Office() {
        super();
    }

    public Office(long id, String code, String floor, String zone,
                  int capacity, WorkspaceStatus status,
                  Long assignedMemberId, LocalDate assignStart, LocalDate assignEnd,
                  BigDecimal monthlyPrice, String facilities) {
        super(id, code, floor, zone, capacity, status,
                assignedMemberId, assignStart, assignEnd);
        this.monthlyPrice = monthlyPrice;
        setFacilities(facilities);
    }

    @Override
    public WorkspaceType getType() { return WorkspaceType.OFFICE; }

    @Override
    public BigDecimal getPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
}
