package com.time.coworking.ui;

import com.time.coworking.controller.ReportController;
import com.time.coworking.dto.ReportRow;
import com.time.coworking.util.Dialogs;
import com.time.coworking.util.ReportPdfExporter;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportsView {

    private final ReportController controller = new ReportController();

    private final Label totalRowsVal = new Label("0");
    private final Label reportVal    = new Label("—");
    private final Label rangeVal     = new Label("—");

    private final DatePicker fromDate = new DatePicker(LocalDate.now().minusMonths(1));
    private final DatePicker toDate   = new DatePicker(LocalDate.now());

    private final TabPane tabPane = new TabPane();

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("rep-root");
        root.setPadding(new Insets(0));

        String css = getClass().getResource("/css/reports.css").toExternalForm();
        root.getStylesheets().add(css);
        root.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null && !sc.getStylesheets().contains(css)) sc.getStylesheets().add(css);
        });

        StackPane hero = buildHero();

        // ── Date range toolbar ──
        fromDate.setPrefWidth(150);
        fromDate.getStyleClass().add("rep-date");
        toDate.setPrefWidth(150);
        toDate.getStyleClass().add("rep-date");

        Label fromLbl = new Label("FROM");
        fromLbl.getStyleClass().add("rep-toolbar-label");
        Label toLbl = new Label("TO");
        toLbl.getStyleClass().add("rep-toolbar-label");

        Button apply = primary("Apply Range", this::reloadAll);
        Button refresh = secondary("Refresh", this::reloadAll);

        HBox toolbar = new HBox(8, fromLbl, fromDate, toLbl, toDate,
                spacer(), apply, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("rep-toolbar");

        // ── Tabs ──
        tabPane.getStyleClass().add("rep-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        tabPane.getTabs().addAll(
                buildDailyBookingsTab(),
                buildMembersTab(),
                buildRevenueTab(),
                buildUtilizationTab());

        tabPane.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> updateHeroStats());

        root.getChildren().addAll(hero, toolbar, tabPane);
        reloadAll();
        playIntro(hero, toolbar, tabPane);
        return root;
    }

    // ───────────── Hero ─────────────

    private StackPane buildHero() {
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        Circle c1 = circle(120, 760, -30, "rep-circle");
        Circle c2 = circle(70, 560, 170, "rep-circle");
        Circle c3 = circle(30, 880, 120, "rep-circle-gold");
        Circle c4 = circle(10, 60, 40, "rep-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label eyebrow  = new Label("REPORTS");
        eyebrow.getStyleClass().add("rep-hero-eyebrow");
        Label headline = new Label("Reports");
        headline.getStyleClass().add("rep-hero-headline");
        Label sub      = new Label("Daily bookings, member activity, revenue and workspace utilization");
        sub.getStyleClass().add("rep-hero-subtitle");
        VBox left = new VBox(6, eyebrow, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                stat("ROWS",   totalRowsVal),
                stat("REPORT", reportVal),
                stat("RANGE",  rangeVal));
        stats.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, stats);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("rep-hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("rep-hero");
        hero.setMinHeight(130);
        hero.setPrefHeight(130);
        hero.setMaxHeight(130);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(hero.widthProperty());
        clip.heightProperty().bind(hero.heightProperty());
        hero.setClip(clip);
        return hero;
    }

    private VBox stat(String caption, Label value) {
        value.getStyleClass().add("rep-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("rep-stat-caption");
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("rep-stat");
        return box;
    }

    private Circle circle(double r, double x, double y, String styleClass) {
        Circle c = new Circle(r);
        c.setLayoutX(x);
        c.setLayoutY(y);
        c.getStyleClass().add(styleClass);
        return c;
    }

    private void drift(Node n, double seconds, double dx, double dy) {
        TranslateTransition tt = new TranslateTransition(Duration.seconds(seconds), n);
        tt.setByX(dx);
        tt.setByY(dy);
        tt.setAutoReverse(true);
        tt.setCycleCount(Timeline.INDEFINITE);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    private void playIntro(Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            Node n = nodes[i];
            n.setOpacity(0);
            n.setTranslateY(14);
            FadeTransition f = new FadeTransition(Duration.millis(380), n);
            f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(380), n);
            t.setToY(0);
            t.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition p = new ParallelTransition(f, t);
            PauseTransition delay = new PauseTransition(Duration.millis(80 + i * 110L));
            delay.setOnFinished(e -> p.play());
            delay.play();
        }
    }

    // ───────────── Tabs ─────────────

    private Tab buildDailyBookingsTab() {
        return buildReportTab("Daily Bookings", "No bookings in the selected range",
                "daily-bookings-report", "Daily Booking Report");
    }

    private Tab buildMembersTab() {
        return buildReportTab("Members", "No members",
                "member-report", "Member Report");
    }

    private Tab buildRevenueTab() {
        return buildReportTab("Revenue", "No revenue in the selected range",
                "revenue-report", "Revenue Report");
    }

    private Tab buildUtilizationTab() {
        return buildReportTab("Utilization", "No utilization data",
                "workspace-utilization-report", "Workspace Utilization Report");
    }

    private Tab buildReportTab(String tabName, String emptyText,
                               String defaultFileName, String pdfTitle) {
        TableView<ReportRow> table = new TableView<>();
        setupTable(table);
        Label empty = new Label(emptyText);
        empty.getStyleClass().add("rep-empty");
        table.setPlaceholder(empty);

        Button export = primary("Export PDF", () -> exportPdf(table, pdfTitle, defaultFileName));
        HBox bar = new HBox(10, export);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(0, 10, 0, 10));

        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().add("rep-table-card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        VBox content = new VBox(10, bar, tableCard);
        content.setPadding(new Insets(10));

        Tab tab = new Tab(tabName, content);
        tab.setUserData(table);
        return tab;
    }

    private void exportPdf(TableView<ReportRow> table, String pdfTitle, String defaultFileName) {
        List<ReportRow> rows = table.getItems();
        if (rows == null || rows.isEmpty()) {
            Dialogs.warn("Nothing to export", "There is no data in this report.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save " + pdfTitle);
        chooser.setInitialFileName(defaultFileName + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File out = chooser.showSaveDialog(null);
        if (out == null) return;

        String from = fromDate.getValue() == null ? "" : fromDate.getValue().toString();
        String to   = toDate.getValue() == null ? "" : toDate.getValue().toString();
        String subtitle = "Range: " + from + " → " + to + "  ·  Rows: " + rows.size();

        try {
            ReportPdfExporter.export(pdfTitle, subtitle, rows, out);
            Dialogs.info("PDF exported", "Saved to:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            Dialogs.error("Export failed", ex.getMessage());
        }
    }

    // ───────────── Table setup ─────────────

    private void setupTable(TableView<ReportRow> table) {
        table.getStyleClass().add("rep-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(42);
    }

    // ───────────── Data loading ─────────────

    private void reloadAll() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null) return;
        if (to.isBefore(from)) {
            Dialogs.warn("Invalid range", "The 'to' date must be on or after the 'from' date.");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                var daily = controller.dailyBookings(from, to);
                var members = controller.members();
                var revenue = controller.revenue(from, to);
                var utilization = controller.utilization(from, to);

                javafx.application.Platform.runLater(() -> {
                    applyReport(tabPane.getTabs().get(0), daily);
                    applyReport(tabPane.getTabs().get(1), members);
                    applyReport(tabPane.getTabs().get(2), revenue);
                    applyReport(tabPane.getTabs().get(3), utilization);
                    updateHeroStats();
                });
                return null;
            }
        };
        task.setOnFailed(e -> Dialogs.error("Load failed", task.getException().getMessage()));
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    @SuppressWarnings("unchecked")
    private void applyReport(Tab tab, List<ReportRow> rows) {
        TableView<ReportRow> table = (TableView<ReportRow>) tab.getUserData();
        if (table == null) return;

        table.getColumns().clear();
        if (rows == null || rows.isEmpty()) {
            table.setItems(FXCollections.emptyObservableList());
            return;
        }

        List<String> columns = new ArrayList<>(rows.get(0).values().keySet());
        for (String col : columns) {
            TableColumn<ReportRow, Object> tc = new TableColumn<>(prettyHeader(col));
            tc.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().get(col)));
            tc.setPrefWidth(estimateColumnWidth(col));
            table.getColumns().add(tc);
        }
        table.setItems(FXCollections.observableArrayList(rows));
    }

    private String prettyHeader(String raw) {
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : raw.toCharArray()) {
            if (c == '_') { sb.append(' '); cap = true; }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else sb.append(c);
        }
        return sb.toString();
    }

    private double estimateColumnWidth(String col) {
        return switch (col) {
            case "booking_date", "payment_day", "join_date", "expiry_date" -> 140;
            case "email"                                                    -> 220;
            case "full_name", "company"                                     -> 180;
            case "workspace_code", "workspace_type"                         -> 140;
            default                                                         -> 120;
        };
    }

    private void updateHeroStats() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        reportVal.setText(selected.getText());

        @SuppressWarnings("unchecked")
        TableView<ReportRow> table = (TableView<ReportRow>) selected.getUserData();
        if (table != null) totalRowsVal.setText(String.valueOf(table.getItems().size()));

        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from != null && to != null) rangeVal.setText(from + " → " + to);
    }

    // ───────────── Helpers ─────────────

    private Button primary(String t, Runnable a)   { return btn(t, "rep-btn-primary", a); }
    private Button secondary(String t, Runnable a) { return btn(t, "rep-btn-secondary", a); }

    private Button btn(String t, String styleClass, Runnable a) {
        Button b = new Button(t);
        b.getStyleClass().add(styleClass);
        b.setOnAction(e -> a.run());
        return b;
    }

    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }
}