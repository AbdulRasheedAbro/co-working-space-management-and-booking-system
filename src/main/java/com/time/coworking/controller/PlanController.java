package com.time.coworking.controller;

import com.time.coworking.model.MembershipPlan;
import com.time.coworking.service.MembershipPlanService;

import java.util.List;

/**
 * Thin translation layer between JavaFX and MembershipPlanService.
 */
public class PlanController {

    private final MembershipPlanService service = new MembershipPlanService();

    public List<MembershipPlan> list()                   { return service.findAll(); }
    public List<MembershipPlan> active()                 { return service.findActive(); }
    public MembershipPlan get(long id)                   { return service.findById(id); }
    public MembershipPlan add(MembershipPlan p)          { return service.add(p); }
    public MembershipPlan update(MembershipPlan p)       { return service.update(p); }
    public void toggleActive(long id)                    { service.toggleActive(id); }
    public void delete(long id)                          { service.delete(id); }
}