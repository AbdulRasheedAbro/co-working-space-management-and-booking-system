package com.time.coworking.ui;

import com.time.coworking.controller.InvoiceController;
import com.time.coworking.model.Invoice;
import com.time.coworking.model.InvoiceItem;
import com.time.coworking.util.Dialogs;
import com.time.coworking.util.PdfExporter;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.text.DecimalFormat;
import java.util.function.Function;

public class InvoiceDetailView {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final Invoice invoice;
    private final InvoiceController controller = new InvoiceController();

    public InvoiceDetailView(Invoice invoice) {
        this.invoice = invoice;
    }

    public void showAndWait() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Invoice " + invoice.getInvoiceNumber());
        dialog.setResizable(false);

        // ── Header ──
        Label title = new Label(invoice.getInvoiceNumber());
        title.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill:#0f172a;");
        Label sub = new Label("Issued " + invoice.getIssueDate()
                + "  ·  Status: " + invoice.getStatus());
        sub.setStyle("-fx-text-fill:#64748b;-fx-font-size:13px;");
        VBox header = new VBox(6, title, sub);
        header.setPadding(new Insets(22, 22, 12, 22));

        // ── Items table ──
        TableView<InvoiceItem> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(40);
        table.setPlaceholder(new Label("No items"));

        table.getColumns().addAll(
                colItem("Description", it -> it.getDescription(), 300),
                colItem("Qty",         it -> it.getQuantity(),      70),
                colItem("Unit Price",  it -> "PKR " + MONEY.format(it.getUnitPrice()), 140),
                colItem("Line Total",  it -> "PKR " + MONEY.format(it.getLineTotal()), 140)
        );

        var items = controller.items(invoice.getId());
        table.setItems(FXCollections.observableArrayList(items));
        VBox.setVgrow(table, Priority.ALWAYS);

        // ── Totals ──
        Label subtotal = new Label("Subtotal:   PKR " + MONEY.format(invoice.getSubtotal()));
        Label discount = new Label("Discount:   PKR " + MONEY.format(invoice.getDiscount()));
        Label total    = new Label("Total:      PKR " + MONEY.format(invoice.getTotal()));
        total.setStyle("-fx-font-weight:800;-fx-font-size:15px;-fx-text-fill:#065f46;");

        VBox totals = new VBox(6, subtotal, discount, total);
        totals.setAlignment(Pos.CENTER_RIGHT);
        totals.setPadding(new Insets(12, 22, 12, 22));

        // ── Buttons ──
        Button download = new Button("Download PDF");
        download.getStyleClass().add("primary-button");
        download.setOnAction(e -> downloadPdf(items));

        Button close = new Button("Close");
        close.getStyleClass().add("secondary-button");
        close.setOnAction(e -> dialog.close());

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox buttons = new HBox(10, grow, close, download);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(0, 22, 22, 22));

        VBox root = new VBox(header, table, totals, buttons);
        root.setStyle("-fx-background-color:white;");
        root.setPrefWidth(720);
        root.setPrefHeight(560);

        Scene scene = new Scene(root);
        try { scene.getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm()); }
        catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void downloadPdf(java.util.List<InvoiceItem> items) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Invoice PDF");
        chooser.setInitialFileName(invoice.getInvoiceNumber() + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File out = chooser.showSaveDialog(null);
        if (out == null) return;

        try {
            PdfExporter.exportInvoice(invoice, items, out);
            Dialogs.info("PDF saved", "Saved to:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            Dialogs.error("PDF failed", ex.getMessage());
        }
    }

    private <T> TableColumn<InvoiceItem, T> colItem(String title,
                                                    Function<InvoiceItem, T> getter,
                                                    double width) {
        TableColumn<InvoiceItem, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}