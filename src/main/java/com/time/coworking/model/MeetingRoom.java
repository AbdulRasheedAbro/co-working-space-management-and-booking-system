package com.time.coworking.model;

import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.enums.WorkspaceType;

import java.math.BigDecimal;

public class MeetingRoom extends Workspace {

    private BigDecimal hourlyPrice;

    public MeetingRoom() {
        super();
    }

    public MeetingRoom(long id, String code, String floor, String zone,
                       int capacity, WorkspaceStatus status,
                       BigDecimal hourlyPrice, String facilities) {
        super(id, code, floor, zone, capacity, status, null, null, null);
        this.hourlyPrice = hourlyPrice;
        setFacilities(facilities);
    }

    @Override
    public WorkspaceType getType() { return WorkspaceType.MEETING_ROOM; }

    @Override
    public BigDecimal getPrice() { return hourlyPrice; }

    public BigDecimal getHourlyPrice() { return hourlyPrice; }
    public void setHourlyPrice(BigDecimal hourlyPrice) { this.hourlyPrice = hourlyPrice; }
}