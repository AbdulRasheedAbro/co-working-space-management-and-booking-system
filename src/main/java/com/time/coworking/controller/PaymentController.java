package com.time.coworking.controller;

import com.time.coworking.enums.PaymentMethod;
import com.time.coworking.enums.PaymentStatus;
import com.time.coworking.model.Payment;
import com.time.coworking.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PaymentController {

    private final PaymentService service = new PaymentService();

    public List<Payment> list()                                        { return service.findAll(); }
    public List<Payment> search(Long memberId, LocalDate from, LocalDate to,
                                PaymentStatus status)                  { return service.search(memberId, from, to, status); }
    public Payment get(long id)                                        { return service.findById(id); }

    public Payment record(long memberId, Long invoiceId, BigDecimal amount,
                          PaymentMethod method, String referenceNo)    {
        return service.record(memberId, invoiceId, amount, method, referenceNo);
    }

    public void cancel(long id)                                        { service.cancel(id); }
    public void refund(long id)                                        { service.refund(id); }
    public BigDecimal totalPaidBetween(LocalDate from, LocalDate to)   { return service.totalPaidBetween(from, to); }
}