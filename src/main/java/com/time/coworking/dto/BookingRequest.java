package com.time.coworking.dto;

import com.time.coworking.enums.PaymentMethod;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public record BookingRequest(
        long memberId,
        long workspaceId,
        LocalDate startDate,       // renamed from bookingDate
        LocalDate endDate,         // NEW — inclusive
        LocalTime startTime,
        LocalTime endTime,
        boolean payNow,
        PaymentMethod paymentMethod
) {
    /** Number of days in this booking (inclusive). */
    public long dayCount() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** True if this is a single-day booking. */
    public boolean isSingleDay() {
        return startDate.equals(endDate);
    }
}