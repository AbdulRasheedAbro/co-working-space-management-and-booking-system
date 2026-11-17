package com.time.coworking.model;

import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.enums.WorkspaceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Desk extends Workspace {

    private WorkspaceType type;
    private BigDecimal monthlyPrice;

    public Desk() {
        super();
    }

    /**
     * Full constructor — 11 arguments including facilities.
     * WorkspaceRepository calls this one.
     */
    public Desk(long id, String code, String floor, String zone,
                WorkspaceStatus status, Long assignedMemberId,
                LocalDate assignStart, LocalDate assignEnd,
                WorkspaceType type, BigDecimal monthlyPrice, String facilities) {
        super(id, code, floor, zone, 1, status,
                assignedMemberId, assignStart, assignEnd);
        if (type != WorkspaceType.HOT_DESK && type != WorkspaceType.DEDICATED_DESK) {
            throw new IllegalArgumentException("Desk type must be HOT_DESK or DEDICATED_DESK");
        }
        this.type = type;
        this.monthlyPrice = monthlyPrice;
        setFacilities(facilities);
    }

    @Override
    public WorkspaceType getType() { return type; }
    public void setType(WorkspaceType type) { this.type = type; }

    @Override
    public BigDecimal getPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
}