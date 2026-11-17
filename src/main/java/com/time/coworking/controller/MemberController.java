package com.time.coworking.controller;

import com.time.coworking.enums.MemberStatus;
import com.time.coworking.model.Member;
import com.time.coworking.service.MemberService;

import java.util.List;

/**
 * Thin translation layer between JavaFX and MemberService.
 * No business logic lives here.
 */
public class MemberController {

    private final MemberService service = new MemberService();

    public List<Member> list() {
        return service.findAll();
    }

    public List<Member> search(String q, MemberStatus status) {
        return service.search(q, status);
    }

    public Member get(long id) {
        return service.findById(id);
    }

    public Member add(Member m) {
        return service.add(m);
    }

    public Member update(Member m) {
        return service.update(m);
    }

    public void delete(long id) {
        service.delete(id);
    }

    public Member renew(long id, int days) {
        return service.renew(id, days);
    }
}