package com.time.coworking.controller;

import com.time.coworking.model.CheckIn;
import com.time.coworking.service.CheckInService;

import java.util.List;

public class CheckInController {

    private final CheckInService service = new CheckInService();

    public List<CheckIn> list()              { return service.findAll(); }
    public List<CheckIn> today()             { return service.findToday(); }
    public CheckIn checkIn(long bookingId)   { return service.checkIn(bookingId); }
    public CheckIn checkOut(long bookingId)  { return service.checkOut(bookingId); }
    public CheckIn findByBooking(long id)    { return service.findByBooking(id); }
}