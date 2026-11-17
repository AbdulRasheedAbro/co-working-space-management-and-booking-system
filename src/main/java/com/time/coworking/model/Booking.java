package com.time.coworking.model;

import com.time.coworking.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class Booking {

    private long id;
    private String bookingCode;
    private long memberId;
    private long workspaceId;
    private LocalDate bookingDate;   // start date
    private LocalDate endDate;       // inclusive end date
    private LocalTime startTime;
    private LocalTime endTime;
    private int durationMin;
    private BookingStatus status;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public Booking() {
    }

    public Booking(long id, String bookingCode, long memberId, long workspaceId,
                   LocalDate bookingDate, LocalDate endDate,
                   LocalTime startTime, LocalTime endTime,
                   int durationMin, BookingStatus status, BigDecimal amount,
                   LocalDateTime createdAt) {
        this.id = id;
        this.bookingCode = bookingCode;
        this.memberId = memberId;
        this.workspaceId = workspaceId;
        this.bookingDate = bookingDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMin = durationMin;
        this.status = status;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public long getMemberId() { return memberId; }
    public void setMemberId(long memberId) { this.memberId = memberId; }

    public long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(long workspaceId) { this.workspaceId = workspaceId; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking booking)) return false;
        return id == booking.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return bookingCode; }
}