package com.time.coworking.ui;

import com.time.coworking.controller.InvoiceController;
import com.time.coworking.enums.InvoiceStatus;
import com.time.coworking.model.Invoice;
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
import java.util.List;
import java.util.function.Function;

public class InvoicesView {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final InvoiceController controller = new InvoiceController();
    private final ObservableList<Invoice> data = FXCollections.observableArrayList();

    private TableView<Invoice> table;
    private DatePicker fromDate, toDate;
    private ComboBox<String> statusFilter;
    private final Label totalVal    = new Label("0");
    private final Label paidVal     = new Label("0");
    private final Label unpaidVal   = new Label("0");

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("inv-root");
        root.setPadding(new Insets(0));

        String css = getClass().getResource("/css/invoices.css").toExternalForm();
        root.getStylesheets().add(css);
        root.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null && !sc.getStylesheets().contains(css)) sc.getStylesheets().add(css);
        });

        StackPane hero = buildHero();

        fromDate = new DatePicker(LocalDate.now().minusMonths(1));
        fromDate.setPrefWidth(150);
        fromDate.getStyleClass().add("inv-date");
        fromDate.setOnAction(e -> load());

        toDate = new DatePicker(LocalDate.now());
        toDate.setPrefWidth(150);
        toDate.getStyleClass().add("inv-date");
        toDate.setOnAction(e -> load());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "DRAFT", "ISSUED", "PAID", "PARTIALLY_PAID", "CANCELLED");
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(160);
        statusFilter.getStyleClass().add("inv-combo");
        statusFilter.setCellFactory(lv -> prettyCell());
        statusFilter.setButtonCell(prettyCell());
        statusFilter.setOnAction(e -> load());

        Button view   = primary("View / PDF", this::openDetail);
        Button refresh = secondary("Refresh", this::load);

        Label fromLbl = new Label("FROM");
        fromLbl.getStyleClass().add("inv-toolbar-label");
        Label toLbl = new Label("TO");
        toLbl.getStyleClass().add("inv-toolbar-label");
        Label stLbl = new Label("STATUS");
        stLbl.getStyleClass().add("inv-toolbar-label");

        HBox toolbar = new HBox(8,
                fromLbl, fromDate, toLbl, toDate, stLbl, statusFilter,
                spacer(), view, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("inv-toolbar");

        table = new TableView<>(data);
        table.getStyleClass().add("inv-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        Label empty = new Label("No invoices found");
        empty.getStyleClass().add("inv-empty");
        table.setPlaceholder(empty);
        table.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openDetail();
            });
            return row;
        });

        TableColumn<Invoice, String> statusCol = col("Status", i -> i.getStatus().name(), 130);
        statusCol.setCellFactory(c -> badgeCell());

        table.getColumns().addAll(
                col("Invoice #",     i -> i.getInvoiceNumber(),              180),
                col("Member ID",     i -> i.getMemberId(),                   110),
                col("Issue Date",    i -> String.valueOf(i.getIssueDate()),  130),
                col("Subtotal",      i -> "PKR " + MONEY.format(i.getSubtotal()), 140),
                col("Discount",      i -> "PKR " + MONEY.format(i.getDiscount()), 130),
                col("Total",         i -> "PKR " + MONEY.format(i.getTotal()),    140),
                statusCol
        );
        table.getColumns().get(0).getStyleClass().add("inv-col-number");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(table.widthProperty());
        clip.heightProperty().bind(table.heightProperty());
        table.setClip(clip);

        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().add("inv-table-card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        root.getChildren().addAll(hero, toolbar, tableCard);
        load();
        playIntro(hero, toolbar, tableCard);
        return root;
    }

    private StackPane buildHero() {
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        Circle c1 = circle(120, 760, -30, "inv-circle");
        Circle c2 = circle(70, 560, 170, "inv-circle");
        Circle c3 = circle(30, 880, 120, "inv-circle-gold");
        Circle c4 = circle(10, 60, 40, "inv-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label eyebrow  = new Label("INVOICES");
        eyebrow.getStyleClass().add("inv-hero-eyebrow");
        Label headline = new Label("Invoices");
        headline.getStyleClass().add("inv-hero-headline");
        Label sub      = new Label("Browse, view and export invoices as PDF");
        sub.getStyleClass().add("inv-hero-subtitle");
        VBox left = new VBox(6, eyebrow, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                stat("SHOWN",  totalVal),
                stat("PAID",   paidVal),
                stat("UNPAID", unpaidVal));
        stats.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, stats);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("inv-hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("inv-hero");
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
        value.getStyleClass().add("inv-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("inv-stat-caption");
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("inv-stat");
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
        LocalDate to = toDate.getValue();
        InvoiceStatus st = "All".equals(statusFilter.getValue())
                ? null : InvoiceStatus.valueOf(statusFilter.getValue());

        Task<List<Invoice>> task = new Task<>() {
            @Override protected List<Invoice> call() {
                return controller.search(from, to, st);
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
        long paid = data.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
        totalVal.setText(String.valueOf(data.size()));
        paidVal.setText(String.valueOf(paid));
        unpaidVal.setText(String.valueOf(data.size() - paid));
    }

    private void openDetail() {
        Invoice inv = table.getSelectionModel().getSelectedItem();
        if (inv == null) { Dialogs.warn("No selection", "Select an invoice first."); return; }
        new InvoiceDetailView(inv).showAndWait();
    }

    private ListCell<String> prettyCell() {
        return new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.replace('_', ' '));
                getStyleClass().remove("inv-list-cell");
                getStyleClass().add("inv-list-cell");
            }
        };
    }

    private TableCell<Invoice, String> badgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item.replace('_', ' '));
                b.getStyleClass().addAll("inv-badge", statusClass(item));
                setGraphic(b);
                setText(null);
            }
        };
    }

    private String statusClass(String s) {
        return switch (s) {
            case "PAID"           -> "inv-badge-green";
            case "ISSUED"         -> "inv-badge-teal";
            case "DRAFT"          -> "inv-badge-amber";
            case "PARTIALLY_PAID" -> "inv-badge-amber";
            case "CANCELLED"      -> "inv-badge-red";
            default               -> "inv-badge-slate";
        };
    }

    private Button primary(String t, Runnable a)   { return btn(t, "inv-btn-primary", a); }
    private Button secondary(String t, Runnable a) { return btn(t, "inv-btn-secondary", a); }

    private Button btn(String t, String styleClass, Runnable a) {
        Button b = new Button(t);
        b.getStyleClass().add(styleClass);
        b.setOnAction(e -> a.run());
        return b;
    }

    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    private <T> TableColumn<Invoice, T> col(String title,
                                            Function<Invoice, T> getter,
                                            double width) {
        TableColumn<Invoice, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}