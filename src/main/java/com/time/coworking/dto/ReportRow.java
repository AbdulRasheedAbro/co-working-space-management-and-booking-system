package com.time.coworking.dto;

import java.util.Map;

/**
 * Generic row for report tables — one map of column header → value.
 * Lets us reuse one TableView for all four reports without creating
 * four separate DTOs.
 */
public record ReportRow(Map<String, Object> values) {
    public Object get(String column) { return values.get(column); }
}