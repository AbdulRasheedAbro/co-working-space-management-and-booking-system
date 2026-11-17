package com.time.coworking.util;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.time.coworking.model.Invoice;
import com.time.coworking.model.InvoiceItem;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public final class PdfExporter {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private PdfExporter() {}

    public static File exportInvoice(Invoice invoice, List<InvoiceItem> items, File outFile)
            throws IOException {
        try (PdfWriter writer = new PdfWriter(outFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            // ── Header ──
            Paragraph brand = new Paragraph("TIME Co-working")
                    .setBold().setFontSize(22).setFontColor(ColorConstants.DARK_GRAY);
            Paragraph tagline = new Paragraph("Management & Booking System")
                    .setFontSize(10).setFontColor(ColorConstants.GRAY);
            doc.add(brand);
            doc.add(tagline);
            doc.add(new Paragraph("\n"));

            doc.add(new Paragraph("INVOICE")
                    .setBold().setFontSize(18)
                    .setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber())
                    .setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("Date: " + invoice.getIssueDate())
                    .setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("Status: " + invoice.getStatus())
                    .setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("\n"));

            // ── Items table ──
            Table table = new Table(UnitValue.createPercentArray(new float[]{5, 2, 2, 2}))
                    .useAllAvailableWidth();

            table.addHeaderCell(header("Description"));
            table.addHeaderCell(header("Qty"));
            table.addHeaderCell(header("Unit Price"));
            table.addHeaderCell(header("Line Total"));

            for (InvoiceItem item : items) {
                table.addCell(cell(item.getDescription()));
                table.addCell(cell(String.valueOf(item.getQuantity())));
                table.addCell(cell("PKR " + MONEY.format(item.getUnitPrice())));
                table.addCell(cell("PKR " + MONEY.format(item.getLineTotal())));
            }

            doc.add(table);
            doc.add(new Paragraph("\n"));

            // ── Totals ──
            Table totals = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                    .useAllAvailableWidth();
            totals.addCell(labelCell("Subtotal"));
            totals.addCell(valueCell("PKR " + MONEY.format(invoice.getSubtotal())));
            totals.addCell(labelCell("Discount"));
            totals.addCell(valueCell("PKR " + MONEY.format(invoice.getDiscount())));
            totals.addCell(boldLabelCell("Total"));
            totals.addCell(boldValueCell("PKR " + MONEY.format(invoice.getTotal())));

            doc.add(totals);
            doc.add(new Paragraph("\n"));

            doc.add(new Paragraph("Thank you for choosing TIME Co-working.")
                    .setItalic().setFontSize(10).setFontColor(ColorConstants.GRAY));

            return outFile;
        }
    }

    private static Cell header(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setPadding(6).setBorder(Border.NO_BORDER);
    }

    private static Cell cell(String text) {
        return new Cell().add(new Paragraph(text == null ? "" : text))
                .setPadding(6).setBorder(Border.NO_BORDER);
    }

    private static Cell labelCell(String text) {
        return new Cell().add(new Paragraph(text))
                .setPadding(4).setBorder(Border.NO_BORDER);
    }

    private static Cell valueCell(String text) {
        return new Cell().add(new Paragraph(text))
                .setPadding(4).setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
    }

    private static Cell boldLabelCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                .setPadding(4).setBorder(Border.NO_BORDER);
    }

    private static Cell boldValueCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                .setPadding(4).setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
    }
}