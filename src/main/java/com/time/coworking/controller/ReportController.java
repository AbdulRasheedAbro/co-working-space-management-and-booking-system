package com.time.coworking.controller;

import com.time.coworking.dto.ReportRow;
import com.time.coworking.service.ReportService;

import java.time.LocalDate;
import java.util.List;

public class ReportController {

    private final ReportService service = new ReportService();

    public List<ReportRow> dailyBookings(LocalDate from, LocalDate to) {
        return service.dailyBookingReport(from, to);
    }

    public List<ReportRow> members() {
        return service.memberReport();
    }

    public List<ReportRow> revenue(LocalDate from, LocalDate to) {
        return service.revenueReport(from, to);
    }

    public List<ReportRow> utilization(LocalDate from, LocalDate to) {
        return service.utilizationReport(from, to);
    }
}