package com.time.coworking.dto;

import com.time.coworking.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingDTO(
        long id,
        String bookingCode,
        String memberName,
        String workspaceCode,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        int durationMin,
        BookingStatus status,
        BigDecimal amount
) {}