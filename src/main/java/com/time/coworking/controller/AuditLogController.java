package com.time.coworking.controller;

import com.time.coworking.repository.AuditLogRepository;
import com.time.coworking.repository.AuditLogRepository.AuditRow;

import java.time.LocalDate;
import java.util.List;

public class AuditLogController {

    private final AuditLogRepository repo = new AuditLogRepository();

    public List<AuditRow> list()  { return repo.findAll(); }

    public List<AuditRow> search(String username, String action, String entity, LocalDate date) {
        return repo.search(username, action, entity, date);
    }
}