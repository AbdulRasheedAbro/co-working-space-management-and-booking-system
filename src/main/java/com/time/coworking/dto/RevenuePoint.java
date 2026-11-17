package com.time.coworking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenuePoint(LocalDate day, BigDecimal total) {}