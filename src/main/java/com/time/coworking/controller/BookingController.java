package com.time.coworking.controller;

import com.time.coworking.Session;
import com.time.coworking.dto.BookingRequest;
import com.time.coworking.enums.BookingStatus;
import com.time.coworking.model.Booking;
import com.time.coworking.service.BookingService;

import java.time.LocalDate;
import java.util.List;

public class BookingController {

    private final BookingService service = new BookingService();

    public List<Booking> list()                                        { return service.findAll(); }
    public List<Booking> today()                                       { return service.findToday(); }
    public List<Booking> search(LocalDate from, LocalDate to,
                                Long memberId, Long workspaceId,
                                BookingStatus status)                  { return service.search(from, to, memberId, workspaceId, status); }
    public Booking get(long id)                                        { return service.findById(id); }
    public List<Booking> create(BookingRequest req)                    { return service.create(req, Session.getUsername()); }
    public void cancel(long id)                                        { service.cancel(id); }
}