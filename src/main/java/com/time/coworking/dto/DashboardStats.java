package com.time.coworking.dto;

import java.math.BigDecimal;

public record DashboardStats(
        long totalMembers,
        long activeMembers,
        long expiredMemberships,
        long availableDesks,
        long occupiedDesks,
        long availableOffices,
        long occupiedOffices,
        long todaysBookings,
        long todaysCheckIns,
        BigDecimal monthlyRevenue,
        BigDecimal pendingPayments,
        double occupancyRate
) {}