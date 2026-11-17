package com.time.coworking.controller;

import com.time.coworking.dto.DashboardStats;
import com.time.coworking.service.DashboardService;

public class DashboardController {

    private final DashboardService service = new DashboardService();

    public DashboardStats load() {
        return service.getStats();
    }
}