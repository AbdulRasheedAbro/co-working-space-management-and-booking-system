package com.time.coworking.model;

import com.time.coworking.enums.MemberStatus;

import java.time.LocalDate;
import java.util.Objects;

public class Member {

    private long id;
    private String fullName;
    private String email;
    private String phone;
    private String cnic;
    private String company;
    private String membershipType;
    private LocalDate joinDate;
    private LocalDate expiryDate;
    private MemberStatus status;

    public Member() {
    }

    public Member(long id, String fullName, String email, String phone, String cnic,
                  String company, String membershipType, LocalDate joinDate,
                  LocalDate expiryDate, MemberStatus status) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.cnic = cnic;
        this.company = company;
        this.membershipType = membershipType;
        this.joinDate = joinDate;
        this.expiryDate = expiryDate;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCnic() { return cnic; }
    public void setCnic(String cnic) { this.cnic = cnic; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }

    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member member)) return false;
        return id == member.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Member{" + id + ", '" + fullName + "', " + status + "}";
    }
}