package com.time.coworking.dto;

import com.time.coworking.enums.MemberStatus;

import java.time.LocalDate;

public record MemberDTO(
        long id,
        String fullName,
        String email,
        String phone,
        String cnic,
        String company,
        String membershipType,
        LocalDate joinDate,
        LocalDate expiryDate,
        MemberStatus status
) {}