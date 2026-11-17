package com.time.coworking.controller;

import com.time.coworking.enums.InvoiceStatus;
import com.time.coworking.model.Invoice;
import com.time.coworking.model.InvoiceItem;
import com.time.coworking.service.InvoiceService;

import java.time.LocalDate;
import java.util.List;

public class InvoiceController {

    private final InvoiceService service = new InvoiceService();

    public List<Invoice> list()                                                 { return service.findAll(); }
    public List<Invoice> search(LocalDate from, LocalDate to, InvoiceStatus s)   { return service.search(from, to, s); }
    public List<Invoice> byMember(long memberId)                                 { return service.findByMember(memberId); }
    public Invoice get(long id)                                                  { return service.findById(id); }
    public List<InvoiceItem> items(long invoiceId)                               { return service.findItems(invoiceId); }
}