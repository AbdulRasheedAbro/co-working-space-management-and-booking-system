package com.time.coworking.model;

import java.math.BigDecimal;
import java.util.Objects;

public class InvoiceItem {

    private long id;
    private long invoiceId;
    private String description;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public InvoiceItem() {
    }

    public InvoiceItem(long id, long invoiceId, String description,
                       int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public static InvoiceItem of(long invoiceId, String description,
                                 int qty, BigDecimal unitPrice) {
        return new InvoiceItem(0L, invoiceId, description, qty, unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(qty)));
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(long invoiceId) { this.invoiceId = invoiceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceItem that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}