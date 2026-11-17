package com.time.coworking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class CheckIn {

    private long id;
    private long bookingId;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private Integer actualMin;
    private BigDecimal extraCharge;

    public CheckIn() {
        this.extraCharge = BigDecimal.ZERO;
    }

    public CheckIn(long id, long bookingId) {
        this.id = id;
        this.bookingId = bookingId;
        this.extraCharge = BigDecimal.ZERO;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getBookingId() { return bookingId; }
    public void setBookingId(long bookingId) { this.bookingId = bookingId; }

    public LocalDateTime getCheckinTime() { return checkinTime; }
    public void setCheckinTime(LocalDateTime checkinTime) { this.checkinTime = checkinTime; }

    public LocalDateTime getCheckoutTime() { return checkoutTime; }
    public void setCheckoutTime(LocalDateTime checkoutTime) { this.checkoutTime = checkoutTime; }

    public Integer getActualMin() { return actualMin; }
    public void setActualMin(Integer actualMin) { this.actualMin = actualMin; }

    public BigDecimal getExtraCharge() { return extraCharge; }
    public void setExtraCharge(BigDecimal extraCharge) { this.extraCharge = extraCharge; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckIn checkIn)) return false;
        return id == checkIn.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}