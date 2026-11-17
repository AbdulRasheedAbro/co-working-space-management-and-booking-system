package com.time.coworking.ui;

import com.time.coworking.controller.PaymentController;
import com.time.coworking.enums.PaymentStatus;
import com.time.coworking.model.Payment;
import com.time.coworking.util.Dialogs;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Payments list screen — emerald theme, real data from payments table.
 */
public class PaymentsView {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PaymentController controller = new PaymentController();
    private final ObservableList<Payment> data = FXCollections.observableArrayList();

    private TableView<Payment> table;
    private DatePicker fromDate, toDate;
    private ComboBox<String> statusFilter;
    private final Label totalVal  = new Label("0");
    private final Label paidVal   = new Label("0");
    private final Label refundVal = new Label("0");

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("pay-root");
        root.setPadding(new Insets(0));

        String css = getClass().getResource("/css/payments.css").toExternalForm();
        root.getStylesheets().add(css);
        root.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null && !sc.getStylesheets().contains(css)) sc.getStylesheets().add(css);
        });

        StackPane hero = buildHero();

        // ── Toolbar ──
        fromDate = new DatePicker(LocalDate.now().minusMonths(1));
        fromDate.setPrefWidth(150);
        fromDate.getStyleClass().add("pay-date");
        fromDate.setOnAction(e -> load());

        toDate = new DatePicker(LocalDate.now());
        toDate.setPrefWidth(150);
        toDate.getStyleClass().add("pay-date");
        toDate.setOnAction(e -> load());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "PAID", "PENDING", "PARTIALLY_PAID", "CANCELLED", "REFUNDED");
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(160);
        statusFilter.getStyleClass().add("pay-combo");
        statusFilter.setCellFactory(lv -> prettyCell());
        statusFilter.setButtonCell(prettyCell());
        statusFilter.setOnAction(e -> load());

        Button record  = primary("Record Payment", this::openForm);
        Button refund  = danger("Refund", this::refundSelected);
        Button refresh = secondary("Refresh", this::load);

        Label fromLbl = new Label("FROM");
        fromLbl.getStyleClass().add("pay-toolbar-label");
        Label toLbl = new Label("TO");
        toLbl.getStyleClass().add("pay-toolbar-label");
        Label stLbl = new Label("STATUS");
        stLbl.getStyleClass().add("pay-toolbar-label");

        HBox toolbar = new HBox(8,
                fromLbl, fromDate, toLbl, toDate, stLbl, statusFilter,
                spacer(), record, refund, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("pay-toolbar");

        // ── Table ──
        table = new TableView<>(data);
        table.getStyleClass().add("pay-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        Label empty = new Label("No payments found");
        empty.getStyleClass().add("pay-empty");
        table.setPlaceholder(empty);

        TableColumn<Payment, String> statusCol = col("Status", p -> p.getStatus().name(), 130);
        statusCol.setCellFactory(c -> badgeCell());

        table.getColumns().addAll(
                col("ID",           p -> p.getId(),                                70),
                col("Member ID",    p -> p.getMemberId(),                          110),
                col("Invoice ID",   p -> p.getInvoiceId() == null ? "—" : p.getInvoiceId(), 110),
                col("Amount (PKR)", p -> MONEY.format(p.getAmount()),              140),
                col("Date",         p -> p.getPaymentDate() == null ? "—"
                        : p.getPaymentDate().format(DT),                            180),
                col("Method",       p -> p.getMethod().name().replace('_', ' '),   140),
                statusCol,
                col("Reference",    p -> p.getReferenceNo() == null ? "—" : p.getReferenceNo(), 160)
        );
        table.getColumns().get(0).getStyleClass().add("pay-col-id");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(table.widthProperty());
        clip.heightProperty().bind(table.heightProperty());
        table.setClip(clip);

        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().add("pay-table-card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        root.getChildren().addAll(hero, toolbar, tableCard);
        load();
        playIntro(hero, toolbar, tableCard);
        return root;
    }

    // ───────────── Hero ─────────────

    private StackPane buildHero() {
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        Circle c1 = circle(120, 760, -30, "pay-circle");
        Circle c2 = circle(70, 560, 170, "pay-circle");
        Circle c3 = circle(30, 880, 120, "pay-circle-gold");
        Circle c4 = circle(10, 60, 40, "pay-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label eyebrow  = new Label("PAYMENTS");
        eyebrow.getStyleClass().add("pay-hero-eyebrow");
        Label headline = new Label("Payments");
        headline.getStyleClass().add("pay-hero-headline");
        Label sub      = new Label("Record and track payments — cash, bank transfer, card, online");
        sub.getStyleClass().add("pay-hero-subtitle");
        VBox left = new VBox(6, eyebrow, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                stat("SHOWN",  totalVal),
                stat("PAID",   paidVal),
                stat("REFUND", refundVal));
        stats.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, stats);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("pay-hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("pay-hero");
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
        value.getStyleClass().add("pay-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("pay-stat-caption");
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("pay-stat");
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

    // ───────────── Data ─────────────

    private void load() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        PaymentStatus st = "All".equals(statusFilter.getValue())
                ? null : PaymentStatus.valueOf(statusFilter.getValue());

        Task<List<Payment>> task = new Task<>() {
            @Override protected List<Payment> call() {
                return controller.search(null, from, to, st);
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
        long refunds = data.stream().filter(p -> p.getStatus() == PaymentStatus.REFUNDED).count();
        long paids   = data.stream().filter(p -> p.getStatus() == PaymentStatus.PAID).count();
        totalVal.setText(String.valueOf(data.size()));
        paidVal.setText(String.valueOf(paids));
        refundVal.setText(String.valueOf(refunds));
    }

    private void openForm() {
        PaymentFormView form = new PaymentFormView();
        if (form.showAndWait() != null) load();
    }

    private void refundSelected() {
        Payment p = table.getSelectionModel().getSelectedItem();
        if (p == null) {
            Dialogs.warn("No selection", "Select a payment first.");
            return;
        }
        if (p.getStatus() != PaymentStatus.PAID) {
            Dialogs.warn("Cannot refund", "Only PAID payments can be refunded.");
            return;
        }
        if (!Dialogs.confirm("Refund payment",
                "Refund PKR " + MONEY.format(p.getAmount()) + "?")) return;
        try { controller.refund(p.getId()); load(); }
        catch (Exception ex) { Dialogs.error("Refund failed", ex.getMessage()); }
    }

    // ───────────── Helpers ─────────────

    private ListCell<String> prettyCell() {
        return new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.replace('_', ' '));
                getStyleClass().remove("pay-list-cell");
                getStyleClass().add("pay-list-cell");
            }
        };
    }

    private TableCell<Payment, String> badgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item.replace('_', ' '));
                b.getStyleClass().addAll("pay-badge", statusClass(item));
                setGraphic(b);
                setText(null);
            }
        };
    }

    private String statusClass(String s) {
        return switch (s) {
            case "PAID"           -> "pay-badge-green";
            case "PENDING"        -> "pay-badge-amber";
            case "PARTIALLY_PAID" -> "pay-badge-amber";
            case "CANCELLED"      -> "pay-badge-red";
            case "REFUNDED"       -> "pay-badge-red";
            default               -> "pay-badge-slate";
        };
    }

    private Button primary(String t, Runnable a)   { return btn(t, "pay-btn-primary", a); }
    private Button secondary(String t, Runnable a) { return btn(t, "pay-btn-secondary", a); }
    private Button danger(String t, Runnable a)    { return btn(t, "pay-btn-danger", a); }

    private Button btn(String t, String styleClass, Runnable a) {
        Button b = new Button(t);
        b.getStyleClass().add(styleClass);
        b.setOnAction(e -> a.run());
        return b;
    }

    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    private <T> TableColumn<Payment, T> col(String title,
                                            Function<Payment, T> getter,
                                            double width) {
        TableColumn<Payment, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}