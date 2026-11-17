package com.time.coworking.model;

import com.time.coworking.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Invoice {

    private long id;
    private String invoiceNumber;
    private long memberId;
    private LocalDate issueDate;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private InvoiceStatus status;

    public Invoice() {
    }

    public Invoice(long id, String invoiceNumber, long memberId,
                   LocalDate issueDate, BigDecimal subtotal,
                   BigDecimal discount, BigDecimal total,
                   InvoiceStatus status) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public long getMemberId() { return memberId; }
    public void setMemberId(long memberId) { this.memberId = memberId; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice invoice)) return false;
        return id == invoice.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return invoiceNumber; }
}