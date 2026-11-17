package com.time.coworking.ui;

import com.time.coworking.Session;
import com.time.coworking.controller.BookingController;
import com.time.coworking.enums.BookingStatus;
import com.time.coworking.model.Booking;
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
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

public class BookingsView {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final BookingController controller = new BookingController();
    private final ObservableList<Booking> data = FXCollections.observableArrayList();

    private TableView<Booking> table;
    private DatePicker fromDate;
    private DatePicker toDate;
    private ComboBox<String> statusFilter;
    private final Label totalVal    = new Label("0");
    private final Label todayVal    = new Label("0");
    private final Label confirmedVal = new Label("0");

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("bk-root");
        root.setPadding(new Insets(0));

        String css = getClass().getResource("/css/bookings.css").toExternalForm();
        root.getStylesheets().add(css);
        root.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null && !sc.getStylesheets().contains(css)) sc.getStylesheets().add(css);
        });

        StackPane hero = buildHero();

        fromDate = new DatePicker(LocalDate.now().minusDays(7));
        fromDate.setPrefWidth(150);
        fromDate.getStyleClass().add("bk-date");
        fromDate.setOnAction(e -> load());

        toDate = new DatePicker(LocalDate.now().plusDays(30));
        toDate.setPrefWidth(150);
        toDate.getStyleClass().add("bk-date");
        toDate.setOnAction(e -> load());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "PENDING", "CONFIRMED", "CHECKED_IN",
                "COMPLETED", "CANCELLED", "NO_SHOW");
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(160);
        statusFilter.getStyleClass().add("bk-combo");
        statusFilter.setCellFactory(lv -> prettyCell());
        statusFilter.setButtonCell(prettyCell());
        statusFilter.setOnAction(e -> load());

        Button newBooking = primary("New Booking", () -> openForm());
        Button cancel     = danger("Cancel", this::cancelSelected);
        Button refresh    = secondary("Refresh", this::load);

        Label fromLbl = new Label("FROM");
        fromLbl.getStyleClass().add("bk-toolbar-label");
        Label toLbl = new Label("TO");
        toLbl.getStyleClass().add("bk-toolbar-label");
        Label stLbl = new Label("STATUS");
        stLbl.getStyleClass().add("bk-toolbar-label");

        HBox toolbar = new HBox(8,
                fromLbl, fromDate, toLbl, toDate, stLbl, statusFilter,
                spacer(), newBooking, cancel, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("bk-toolbar");

        table = new TableView<>(data);
        table.getStyleClass().add("bk-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        Label empty = new Label("No bookings found");
        empty.getStyleClass().add("bk-empty");
        table.setPlaceholder(empty);

        TableColumn<Booking, String> statusCol = col("Status", b -> b.getStatus().name(), 130);
        statusCol.setCellFactory(c -> badgeCell());

        table.getColumns().addAll(
                col("Code",         b -> b.getBookingCode(),                 130),
                col("Member ID",    b -> b.getMemberId(),                    100),
                col("Workspace",    b -> b.getWorkspaceId(),                 110),
                col("Start Date",   b -> String.valueOf(b.getBookingDate()), 120),
                col("End Date",     b -> String.valueOf(b.getEndDate()),     120),
                col("Start",        b -> String.valueOf(b.getStartTime()),    90),
                col("End",          b -> String.valueOf(b.getEndTime()),      90),
                col("Duration",     b -> b.getDurationMin() + " min",        100),
                statusCol,
                col("Amount (PKR)", b -> MONEY.format(b.getAmount()),        130)
        );
        table.getColumns().get(0).getStyleClass().add("bk-col-code");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(table.widthProperty());
        clip.heightProperty().bind(table.heightProperty());
        table.setClip(clip);

        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().add("bk-table-card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        root.getChildren().addAll(hero, toolbar, tableCard);
        load();
        playIntro(hero, toolbar, tableCard);
        return root;
    }

    private StackPane buildHero() {
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        Circle c1 = circle(120, 760, -30, "bk-circle");
        Circle c2 = circle(70, 560, 170, "bk-circle");
        Circle c3 = circle(30, 880, 120, "bk-circle-gold");
        Circle c4 = circle(10, 60, 40, "bk-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label eyebrow  = new Label("BOOKINGS");
        eyebrow.getStyleClass().add("bk-hero-eyebrow");
        Label headline = new Label("Bookings");
        headline.getStyleClass().add("bk-hero-headline");
        Label sub      = new Label("Create bookings with conflict-free scheduling and automatic invoicing");
        sub.getStyleClass().add("bk-hero-subtitle");
        VBox left = new VBox(6, eyebrow, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                stat("SHOWN",     totalVal),
                stat("TODAY",     todayVal),
                stat("CONFIRMED", confirmedVal));
        stats.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, stats);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("bk-hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("bk-hero");
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
        value.getStyleClass().add("bk-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("bk-stat-caption");
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("bk-stat");
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

    private void load() {
        LocalDate from = fromDate.getValue();
        LocalDate to   = toDate.getValue();
        BookingStatus st = "All".equals(statusFilter.getValue())
                ? null : BookingStatus.valueOf(statusFilter.getValue());

        Task<List<Booking>> task = new Task<>() {
            @Override protected List<Booking> call() {
                return controller.search(from, to, null, null, st);
            }
        };
        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            updateStats();
        });
        task.setOnFailed(e -> Dialogs.error("Load failed", task.getException().getMessage()));
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void updateStats() {
        LocalDate today = LocalDate.now();
        long todayCount = data.stream()
                .filter(b -> !b.getBookingDate().isAfter(today) && !b.getEndDate().isBefore(today))
                .count();
        long confirmed  = data.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        totalVal.setText(String.valueOf(data.size()));
        todayVal.setText(String.valueOf(todayCount));
        confirmedVal.setText(String.valueOf(confirmed));
    }

    private Booking selected() {
        Booking b = table.getSelectionModel().getSelectedItem();
        if (b == null) Dialogs.warn("No selection", "Select a booking first.");
        return b;
    }

    private void openForm() {
        BookingFormView form = new BookingFormView();
        List<Booking> result = form.showAndWait();
        if (result != null && !result.isEmpty()) load();
    }

    private void cancelSelected() {
        Booking b = selected();
        if (b == null) return;
        if (!Dialogs.confirm("Cancel booking",
                "Cancel booking " + b.getBookingCode() + "?")) return;
        try { controller.cancel(b.getId()); load(); }
        catch (Exception ex) { Dialogs.error("Cancel failed", ex.getMessage()); }
    }

    private ListCell<String> prettyCell() {
        return new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.replace('_', ' '));
                getStyleClass().remove("bk-list-cell");
                getStyleClass().add("bk-list-cell");
            }
        };
    }

    private TableCell<Booking, String> badgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item.replace('_', ' '));
                b.getStyleClass().addAll("bk-badge", statusClass(item));
                setGraphic(b);
                setText(null);
            }
        };
    }

    private String statusClass(String s) {
        return switch (s) {
            case "PENDING"    -> "bk-badge-amber";
            case "CONFIRMED"  -> "bk-badge-green";
            case "CHECKED_IN" -> "bk-badge-teal";
            case "COMPLETED"  -> "bk-badge-slate";
            case "CANCELLED"  -> "bk-badge-red";
            case "NO_SHOW"    -> "bk-badge-red";
            default           -> "bk-badge-slate";
        };
    }

    private Button primary(String t, Runnable a)   { return btn(t, "bk-btn-primary", a); }
    private Button secondary(String t, Runnable a) { return btn(t, "bk-btn-secondary", a); }
    private Button danger(String t, Runnable a)    { return btn(t, "bk-btn-danger", a); }

    private Button btn(String t, String styleClass, Runnable a) {
        Button b = new Button(t);
        b.getStyleClass().add(styleClass);
        b.setOnAction(e -> a.run());
        return b;
    }

    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    private <T> TableColumn<Booking, T> col(String title,
                                            Function<Booking, T> getter,
                                            double width) {
        TableColumn<Booking, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}