package com.time.coworking.ui;

import com.time.coworking.controller.BookingController;
import com.time.coworking.controller.CheckInController;
import com.time.coworking.enums.BookingStatus;
import com.time.coworking.model.Booking;
import com.time.coworking.model.CheckIn;
import com.time.coworking.util.Dialogs;
import javafx.animation.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Check-in / Check-out screen — emerald theme, two-panel layout:
 * top = all check-in records, bottom = pending bookings awaiting check-in.
 */
public class CheckInView {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CheckInController controller = new CheckInController();
    private final BookingController bookingController = new BookingController();

    private final ObservableList<CheckIn> checkins = FXCollections.observableArrayList();
    private final ObservableList<Booking> pending  = FXCollections.observableArrayList();

    private TableView<CheckIn> table;
    private TableView<Booking> pendingTable;
    private final Label totalVal   = new Label("0");
    private final Label activeVal  = new Label("0");
    private final Label pendingVal = new Label("0");

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("ci-root");
        root.setPadding(new Insets(0));

        String css = getClass().getResource("/css/checkins.css").toExternalForm();
        root.getStylesheets().add(css);
        root.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null && !sc.getStylesheets().contains(css)) sc.getStylesheets().add(css);
        });

        StackPane hero = buildHero();

        // ── Toolbar ──
        Button refresh = secondary("Refresh", this::loadAll);
        Button checkIn = primary("Check In Selected", this::doCheckIn);
        Button checkOut = primary("Check Out Selected", this::doCheckOut);

        HBox toolbar = new HBox(10, spacer(), checkIn, checkOut, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("ci-toolbar");

        // ── Table 1: pending bookings (awaiting check-in) ──
        pendingTable = new TableView<>(pending);
        pendingTable.getStyleClass().add("ci-table");
        pendingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        pendingTable.setFixedCellSize(46);
        Label emptyPending = new Label("No bookings awaiting check-in");
        emptyPending.getStyleClass().add("ci-empty");
        pendingTable.setPlaceholder(emptyPending);

        TableColumn<Booking, String> statusCol = colBooking("Status", b -> b.getStatus().name(), 120);
        statusCol.setCellFactory(c -> badgeCellBooking());

        pendingTable.getColumns().addAll(
                colBooking("Code",       b -> b.getBookingCode(),                 120),
                colBooking("Member ID",  b -> b.getMemberId(),                    100),
                colBooking("Workspace",  b -> b.getWorkspaceId(),                 110),
                colBooking("Date",       b -> String.valueOf(b.getBookingDate()), 110),
                colBooking("Start",      b -> String.valueOf(b.getStartTime()),    90),
                colBooking("End",        b -> String.valueOf(b.getEndTime()),      90),
                statusCol
        );
        pendingTable.getColumns().get(0).getStyleClass().add("ci-col-id");

        Rectangle clip1 = roundedClip(pendingTable);
        pendingTable.setClip(clip1);

        Label pendingLbl = new Label("PENDING CHECK-IN");
        pendingLbl.getStyleClass().add("ci-toolbar-label");
        StackPane pendingCard = new StackPane(pendingTable);
        pendingCard.getStyleClass().add("ci-table-card");
        pendingCard.setPrefHeight(280);
        pendingCard.setMaxHeight(280);

        VBox pendingSection = new VBox(8, pendingLbl, pendingCard);

        // ── Table 2: check-in records ──
        table = new TableView<>(checkins);
        table.getStyleClass().add("ci-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        Label empty = new Label("No check-ins found");
        empty.getStyleClass().add("ci-empty");
        table.setPlaceholder(empty);

        table.getColumns().addAll(
                colCheck("ID",             c -> c.getId(),                                 70),
                colCheck("Booking ID",     c -> c.getBookingId(),                          110),
                colCheck("Check-In",       c -> c.getCheckinTime() == null ? "—"
                        : c.getCheckinTime().format(DT),                                   160),
                colCheck("Check-Out",      c -> c.getCheckoutTime() == null ? "—"
                        : c.getCheckoutTime().format(DT),                                  160),
                colCheck("Actual (min)",   c -> c.getActualMin() == null ? "—"
                        : c.getActualMin(),                                                120),
                colCheck("Extra Charge",   c -> MONEY.format(c.getExtraCharge()),          130)
        );
        table.getColumns().get(0).getStyleClass().add("ci-col-id");

        Rectangle clip2 = roundedClip(table);
        table.setClip(clip2);

        Label historyLbl = new Label("CHECK-IN HISTORY");
        historyLbl.getStyleClass().add("ci-toolbar-label");
        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().add("ci-table-card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        VBox historySection = new VBox(8, historyLbl, tableCard);
        VBox.setVgrow(historySection, Priority.ALWAYS);

        root.getChildren().addAll(hero, toolbar, pendingSection, historySection);
        loadAll();
        playIntro(hero, toolbar, pendingSection, historySection);
        return root;
    }

    // ───────────── Hero ─────────────

    private StackPane buildHero() {
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        Circle c1 = circle(120, 760, -30, "ci-circle");
        Circle c2 = circle(70, 560, 170, "ci-circle");
        Circle c3 = circle(30, 880, 120, "ci-circle-gold");
        Circle c4 = circle(10, 60, 40, "ci-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label eyebrow  = new Label("CHECK-IN / CHECK-OUT");
        eyebrow.getStyleClass().add("ci-hero-eyebrow");
        Label headline = new Label("Attendance");
        headline.getStyleClass().add("ci-hero-headline");
        Label sub      = new Label("Record member arrival, departure and overtime charges");
        sub.getStyleClass().add("ci-hero-subtitle");
        VBox left = new VBox(6, eyebrow, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                stat("TOTAL",   totalVal),
                stat("ACTIVE",  activeVal),
                stat("PENDING", pendingVal));
        stats.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, stats);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("ci-hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("ci-hero");
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
        value.getStyleClass().add("ci-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("ci-stat-caption");
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("ci-stat");
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

    private Rectangle roundedClip(Node node) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(node.layoutBoundsProperty().map(r -> r.getWidth()));
        clip.heightProperty().bind(node.layoutBoundsProperty().map(r -> r.getHeight()));
        return clip;
    }

    // ───────────── Data ─────────────

    private void loadAll() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                var all = controller.list();
                var booked = bookingController.search(
                        java.time.LocalDate.now().minusDays(30),
                        java.time.LocalDate.now().plusDays(30),
                        null, null, null);
                var pendingList = booked.stream()
                        .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                        .toList();

                javafx.application.Platform.runLater(() -> {
                    checkins.setAll(all);
                    pending.setAll(pendingList);
                    updateStats();
                });
                return null;
            }
        };
        task.setOnFailed(e -> Dialogs.error("Load failed", task.getException().getMessage()));
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void updateStats() {
        long active = checkins.stream()
                .filter(c -> c.getCheckoutTime() == null && c.getCheckinTime() != null)
                .count();
        totalVal.setText(String.valueOf(checkins.size()));
        activeVal.setText(String.valueOf(active));
        pendingVal.setText(String.valueOf(pending.size()));
    }

    // ───────────── Actions ─────────────

    private void doCheckIn() {
        Booking b = pendingTable.getSelectionModel().getSelectedItem();
        if (b == null) { Dialogs.warn("No selection", "Select a pending booking first."); return; }
        try {
            controller.checkIn(b.getId());
            loadAll();
        } catch (Exception ex) {
            Dialogs.error("Check-in failed", ex.getMessage());
        }
    }

    private void doCheckOut() {
        CheckIn c = table.getSelectionModel().getSelectedItem();
        if (c == null) { Dialogs.warn("No selection", "Select a check-in record first."); return; }
        try {
            controller.checkOut(c.getBookingId());
            loadAll();
        } catch (Exception ex) {
            Dialogs.error("Check-out failed", ex.getMessage());
        }
    }

    // ───────────── Helpers ─────────────

    private TableCell<Booking, String> badgeCellBooking() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item.replace('_', ' '));
                b.getStyleClass().addAll("ci-badge", statusClass(item));
                setGraphic(b);
                setText(null);
            }
        };
    }

    private String statusClass(String s) {
        return switch (s) {
            case "PENDING"    -> "ci-badge-amber";
            case "CONFIRMED"  -> "ci-badge-green";
            case "CHECKED_IN" -> "ci-badge-teal";
            case "COMPLETED"  -> "ci-badge-slate";
            case "CANCELLED"  -> "ci-badge-red";
            case "NO_SHOW"    -> "ci-badge-red";
            default           -> "ci-badge-slate";
        };
    }

    private Button primary(String t, Runnable a)   { return btn(t, "ci-btn-primary", a); }
    private Button secondary(String t, Runnable a) { return btn(t, "ci-btn-secondary", a); }

    private Button btn(String t, String styleClass, Runnable a) {
        Button b = new Button(t);
        b.getStyleClass().add(styleClass);
        b.setOnAction(e -> a.run());
        return b;
    }

    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    private <T> TableColumn<Booking, T> colBooking(String title,
                                                   Function<Booking, T> getter,
                                                   double width) {
        TableColumn<Booking, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    private <T> TableColumn<CheckIn, T> colCheck(String title,
                                                 Function<CheckIn, T> getter,
                                                 double width) {
        TableColumn<CheckIn, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}
