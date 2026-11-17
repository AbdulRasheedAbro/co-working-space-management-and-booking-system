package com.time.coworking.ui;

import com.time.coworking.controller.BookingController;
import com.time.coworking.controller.MemberController;
import com.time.coworking.controller.WorkspaceController;
import com.time.coworking.dto.BookingRequest;
import com.time.coworking.enums.PaymentMethod;
import com.time.coworking.enums.WorkspaceType;
import com.time.coworking.exception.BookingConflictException;
import com.time.coworking.model.Booking;
import com.time.coworking.model.Member;
import com.time.coworking.model.Workspace;
import com.time.coworking.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BookingFormView {

    private final BookingController    bookingController   = new BookingController();
    private final MemberController     memberController    = new MemberController();
    private final WorkspaceController  workspaceController = new WorkspaceController();

    private List<Booking> saved;

    public List<Booking> showAndWait() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New Booking");
        dialog.setResizable(true);

        // ── Member ──
        ComboBox<Member> memberBox = new ComboBox<>();
        memberBox.getItems().setAll(memberController.list());
        memberBox.setConverter(new StringConverter<>() {
            @Override public String toString(Member m) {
                return m == null ? "" : m.getFullName() + "  (" + m.getEmail() + ")";
            }
            @Override public Member fromString(String s) { return null; }
        });
        memberBox.setMaxWidth(Double.MAX_VALUE);

        // ── Workspace type ──
        ComboBox<WorkspaceType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(WorkspaceType.values());
        typeBox.setValue(WorkspaceType.HOT_DESK);
        typeBox.setMaxWidth(Double.MAX_VALUE);

        // ── Workspace ──
        ComboBox<Workspace> workspaceBox = new ComboBox<>();
        workspaceBox.setConverter(new StringConverter<>() {
            @Override public String toString(Workspace w) {
                return w == null ? "" : w.getCode() + "  —  " + w.getType();
            }
            @Override public Workspace fromString(String s) { return null; }
        });
        workspaceBox.setMaxWidth(Double.MAX_VALUE);

        // ── Start + End dates ──
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker endDatePicker   = new DatePicker(LocalDate.now());

        // ── Times ──
        ComboBox<LocalTime> startBox = new ComboBox<>();
        ComboBox<LocalTime> endBox   = new ComboBox<>();
        startBox.getItems().setAll(FXCollections.observableArrayList(
                LocalTime.of(8,0),  LocalTime.of(9,0),  LocalTime.of(10,0),
                LocalTime.of(11,0), LocalTime.of(12,0), LocalTime.of(13,0),
                LocalTime.of(14,0), LocalTime.of(15,0), LocalTime.of(16,0),
                LocalTime.of(17,0), LocalTime.of(18,0), LocalTime.of(19,0),
                LocalTime.of(20,0)));
        endBox.getItems().setAll(startBox.getItems());
        startBox.setValue(LocalTime.of(10,0));
        endBox.setValue(LocalTime.of(12,0));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        startBox.setConverter(new StringConverter<>() {
            @Override public String toString(LocalTime t) { return t == null ? "" : t.format(fmt); }
            @Override public LocalTime fromString(String s) { return null; }
        });
        endBox.setConverter(startBox.getConverter());

        // ── Live info label ──
        Label infoLbl = new Label("—");
        infoLbl.setStyle("-fx-text-fill:#065f46;-fx-font-size:13px;-fx-font-weight:700;");
        infoLbl.setWrapText(true);

        // ── Pay now ──
        CheckBox payNow = new CheckBox("Pay now");
        ComboBox<PaymentMethod> methodBox = new ComboBox<>();
        methodBox.getItems().addAll(PaymentMethod.values());
        methodBox.setValue(PaymentMethod.CASH);
        methodBox.disableProperty().bind(payNow.selectedProperty().not());

        for (Control c : new Control[]{memberBox, typeBox, workspaceBox,
                startDatePicker, endDatePicker, startBox, endBox, methodBox}) {
            c.getStyleClass().add("dlg-input");
        }

        // ── Recompute availability ──
        Runnable refresh = () -> {
            WorkspaceType type = typeBox.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            LocalTime start = startBox.getValue();
            LocalTime end = endBox.getValue();

            if (type == null || startDate == null || endDate == null
                    || start == null || end == null || !end.isAfter(start)) {
                infoLbl.setText("Pick a valid date range and time");
                infoLbl.setStyle("-fx-text-fill:#92400e;-fx-font-size:13px;-fx-font-weight:700;");
                workspaceBox.getItems().clear();
                return;
            }
            if (endDate.isBefore(startDate)) {
                infoLbl.setText("End date must be on or after start date");
                infoLbl.setStyle("-fx-text-fill:#b91c1c;-fx-font-size:13px;-fx-font-weight:700;");
                workspaceBox.getItems().clear();
                return;
            }

            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

            try {
                // Find workspaces free on EVERY day in the range
                List<Workspace> candidates = workspaceController.byType(type);
                List<Workspace> freeForAll = candidates.stream()
                        .filter(w -> {
                            for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                                try {
                                    boolean thisOneFree = workspaceController
                                            .availableForSlot(type, d, start, end)
                                            .stream().anyMatch(x -> x.getId() == w.getId());
                                    if (!thisOneFree) return false;
                                } catch (Exception e) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .toList();

                workspaceBox.getItems().setAll(freeForAll);
                workspaceBox.getSelectionModel().clearSelection();
                if (!freeForAll.isEmpty()) workspaceBox.getSelectionModel().selectFirst();

                long n = freeForAll.size();
                if (n == 0) {
                    infoLbl.setText(days == 1
                            ? "No " + type + " available for " + startDate
                            : "No " + type + " available for all " + days + " days");
                    infoLbl.setStyle("-fx-text-fill:#b91c1c;-fx-font-size:13px;-fx-font-weight:700;");
                } else {
                    String rangeText = days == 1
                            ? startDate.toString()
                            : startDate + " → " + endDate + "  (" + days + " days)";
                    infoLbl.setText(n + " " + type + " available for " + rangeText);
                    infoLbl.setStyle("-fx-text-fill:#065f46;-fx-font-size:13px;-fx-font-weight:700;");
                }
            } catch (Exception ex) {
                infoLbl.setText("Could not check availability: " + ex.getMessage());
                infoLbl.setStyle("-fx-text-fill:#b91c1c;-fx-font-size:13px;-fx-font-weight:700;");
            }
        };

        typeBox.valueProperty().addListener((o, a, b) -> refresh.run());
        startDatePicker.valueProperty().addListener((o, a, b) -> refresh.run());
        endDatePicker.valueProperty().addListener((o, a, b) -> refresh.run());
        startBox.valueProperty().addListener((o, a, b) -> refresh.run());
        endBox.valueProperty().addListener((o, a, b) -> refresh.run());

        // ── Header ──
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        decor.getChildren().addAll(
                circle(90, 520, -20, "dlg-circle"),
                circle(50, 380, 90,  "dlg-circle"),
                circle(22, 470, 70,  "dlg-circle-gold"),
                circle(8,  250, 24,  "dlg-circle-gold"));

        Label logoText = new Label("T");
        logoText.getStyleClass().add("brand-logo-text");
        StackPane logo = new StackPane(logoText);
        logo.getStyleClass().add("brand-logo");

        Label title    = new Label("New Booking");
        title.getStyleClass().add("dlg-title");
        Label subtitle = new Label("Book one day or a full range of days");
        subtitle.getStyleClass().add("dlg-subtitle");

        HBox headerContent = new HBox(14, logo, new VBox(3, title, subtitle));
        headerContent.setAlignment(Pos.CENTER_LEFT);
        headerContent.getStyleClass().add("dlg-header");

        StackPane header = new StackPane(decor, headerContent);
        header.getStyleClass().add("dlg-header-bg");
        Rectangle headerClip = new Rectangle();
        headerClip.widthProperty().bind(header.widthProperty());
        headerClip.heightProperty().bind(header.heightProperty());
        header.setClip(headerClip);

        // ── Body ──
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(14);
        g.getStyleClass().add("dlg-body");
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cc.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(cc);
        }

        int r = 0;
        Node memberField = field("Member *", memberBox);
        GridPane.setColumnSpan(memberField, 2);
        g.add(memberField, 0, r++);

        Node typeField = field("Workspace Type *", typeBox);
        GridPane.setColumnSpan(typeField, 2);
        g.add(typeField, 0, r++);

        Node wsField = field("Workspace *", workspaceBox);
        GridPane.setColumnSpan(wsField, 2);
        g.add(wsField, 0, r++);

        g.add(field("Start Date *", startDatePicker), 0, r);
        g.add(field("End Date *",   endDatePicker),   1, r);
        r++;

        g.add(field("Start Time *", startBox), 0, r);
        g.add(field("End Time *",   endBox),   1, r);
        r++;

        Node infoField = field("Availability", infoLbl);
        GridPane.setColumnSpan(infoField, 2);
        g.add(infoField, 0, r++);

        g.add(field("", payNow), 0, r);
        g.add(field("Payment Method", methodBox), 1, r);

        ScrollPane scroll = new ScrollPane(g);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Buttons ──
        Button book   = new Button("Create Booking");
        book.getStyleClass().add("primary-button");
        book.setDefaultButton(true);
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> dialog.close());

        book.setOnAction(e -> {
            Member m = memberBox.getValue();
            Workspace w = workspaceBox.getValue();
            if (m == null || w == null) {
                Dialogs.warn("Missing input", "Select a member and a workspace first.");
                return;
            }
            LocalTime start = startBox.getValue();
            LocalTime end   = endBox.getValue();
            LocalDate sd = startDatePicker.getValue();
            LocalDate ed = endDatePicker.getValue();

            if (sd == null || ed == null) {
                Dialogs.warn("Invalid dates", "Pick both start and end dates.");
                return;
            }
            if (ed.isBefore(sd)) {
                Dialogs.warn("Invalid dates", "End date must be on or after start date.");
                return;
            }
            if (start == null || end == null || !end.isAfter(start)) {
                Dialogs.warn("Invalid time", "End time must be after start time.");
                return;
            }

            try {
                BookingRequest req = new BookingRequest(
                        m.getId(), w.getId(), sd, ed, start, end,
                        payNow.isSelected(), methodBox.getValue());
                saved = bookingController.create(req);
                dialog.close();
            } catch (BookingConflictException ex) {
                Dialogs.error("Booking Not Allowed", ex.getMessage());
                refresh.run();
            } catch (Exception ex) {
                Dialogs.error("Booking failed", ex.getMessage());
                refresh.run();
            }
        });

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox buttons = new HBox(10, grow, cancel, book);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.getStyleClass().add("dlg-footer");

        VBox root = new VBox(header, scroll, buttons);
        root.getStyleClass().add("dialog-root");
        root.setPrefWidth(640);
        root.setMaxHeight(740);

        double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
        double dialogHeight = Math.min(740, screenHeight - 60);
        root.setPrefHeight(dialogHeight);

        Scene scene = new Scene(root, 640, dialogHeight);
        try { scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm()); }
        catch (Exception ignored) {}
        try { scene.getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm()); }
        catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.setMinWidth(600);
        dialog.setMinHeight(500);
        refresh.run();
        dialog.showAndWait();
        return saved;
    }

    private Circle circle(double r, double x, double y, String styleClass) {
        Circle c = new Circle(r);
        c.setLayoutX(x);
        c.setLayoutY(y);
        c.getStyleClass().add(styleClass);
        return c;
    }

    private VBox field(String labelText, Node input) {
        Label l = new Label(labelText);
        l.getStyleClass().add("field-label");
        return new VBox(6, l, input);
    }
}