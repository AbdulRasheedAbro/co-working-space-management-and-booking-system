package com.time.coworking.ui;

import com.time.coworking.Session;
import com.time.coworking.controller.PlanController;
import com.time.coworking.model.MembershipPlan;
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
import java.util.function.Function;

/**
 * Membership Plans screen — same emerald theme + hero + stats + toolbar + table.
 */
public class PlansView {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final PlanController controller = new PlanController();
    private final ObservableList<MembershipPlan> data = FXCollections.observableArrayList();

    private TableView<MembershipPlan> table;
    private ComboBox<String> activeFilter;
    private final Label totalVal   = new Label("0");
    private final Label activeVal  = new Label("0");
    private final Label inactiveVal = new Label("0");

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("pl-root");
        root.setPadding(new Insets(0));

        String css = getClass().getResource("/css/plans.css").toExternalForm();
        root.getStylesheets().add(css);
        root.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null && !sc.getStylesheets().contains(css)) sc.getStylesheets().add(css);
        });

        StackPane hero = buildHero();

        // ── Toolbar ──
        activeFilter = new ComboBox<>();
        activeFilter.getItems().addAll("All", "Active only", "Inactive only");
        activeFilter.setValue("All");
        activeFilter.setPrefWidth(180);
        activeFilter.getStyleClass().add("pl-combo");
        activeFilter.setCellFactory(lv -> prettyCell());
        activeFilter.setButtonCell(prettyCell());
        activeFilter.setOnAction(e -> load());

        Button add     = primary("Add Plan",     () -> openForm(null));
        Button edit    = secondary("Edit",       () -> openForm(selected()));
        Button toggle  = secondary("Toggle Active", this::toggleSelected);
        Button delete  = danger("Delete",        this::deleteSelected);
        Button refresh = secondary("Refresh",    this::load);

        if (!Session.isAdmin()) {
            add.setDisable(true);
            edit.setDisable(true);
            toggle.setDisable(true);
            delete.setDisable(true);
        }

        Label filterLbl = new Label("FILTER");
        filterLbl.getStyleClass().add("pl-toolbar-label");

        HBox toolbar = new HBox(10,
                filterLbl, activeFilter,
                spacer(), add, edit, toggle, delete, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("pl-toolbar");

        // ── Table ──
        table = new TableView<>(data);
        table.getStyleClass().add("pl-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        Label empty = new Label("No plans found");
        empty.getStyleClass().add("pl-empty");
        table.setPlaceholder(empty);
        table.setRowFactory(tv -> {
            TableRow<MembershipPlan> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && Session.isAdmin()) {
                    openForm(row.getItem());
                }
            });
            return row;
        });

        TableColumn<MembershipPlan, String> activeCol = col("Active", p -> p.isActive() ? "ACTIVE" : "INACTIVE", 120);
        activeCol.setCellFactory(c -> badgeCell());

        table.getColumns().addAll(
                col("ID",          p -> p.getId(),                       70),
                col("Plan Name",   p -> p.getPlanName(),                 220),
                col("Duration",    p -> p.getDurationDays() + " days",   130),
                col("Price (PKR)", p -> MONEY.format(p.getPrice()),      140),
                col("Hours",       p -> p.getAllowedHours(),             100),
                col("Meeting Hrs", p -> p.getIncludedMeetingHours(),     130),
                activeCol,
                col("Description", p -> p.getDescription(),              300)
        );
        table.getColumns().get(0).getStyleClass().add("pl-col-id");
        table.getColumns().get(1).getStyleClass().add("pl-col-name");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(table.widthProperty());
        clip.heightProperty().bind(table.heightProperty());
        table.setClip(clip);

        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().add("pl-table-card");
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
        Circle c1 = circle(120, 760, -30, "pl-circle");
        Circle c2 = circle(70, 560, 170, "pl-circle");
        Circle c3 = circle(30, 880, 120, "pl-circle-gold");
        Circle c4 = circle(10, 60, 40, "pl-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label eyebrow  = new Label("MEMBERSHIP PLANS");
        eyebrow.getStyleClass().add("pl-hero-eyebrow");
        Label headline = new Label("Membership Plans");
        headline.getStyleClass().add("pl-hero-headline");
        Label sub      = new Label("Configure durations, prices and included services");
        sub.getStyleClass().add("pl-hero-subtitle");
        VBox left = new VBox(6, eyebrow, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                stat("SHOWN",    totalVal),
                stat("ACTIVE",   activeVal),
                stat("INACTIVE", inactiveVal));
        stats.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, stats);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("pl-hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("pl-hero");
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
        value.getStyleClass().add("pl-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("pl-stat-caption");
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("pl-stat");
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
        String filter = activeFilter.getValue();

        Task<java.util.List<MembershipPlan>> task = new Task<>() {
            @Override protected java.util.List<MembershipPlan> call() {
                java.util.List<MembershipPlan> all = controller.list();
                if ("Active only".equals(filter)) {
                    return all.stream().filter(MembershipPlan::isActive).toList();
                }
                if ("Inactive only".equals(filter)) {
                    return all.stream().filter(p -> !p.isActive()).toList();
                }
                return all;
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
        long active = data.stream().filter(MembershipPlan::isActive).count();
        totalVal.setText(String.valueOf(data.size()));
        activeVal.setText(String.valueOf(active));
        inactiveVal.setText(String.valueOf(data.size() - active));
    }

    private MembershipPlan selected() {
        MembershipPlan p = table.getSelectionModel().getSelectedItem();
        if (p == null) Dialogs.warn("No selection", "Select a plan first.");
        return p;
    }

    private void openForm(MembershipPlan p) {
        PlanFormView form = new PlanFormView(p);
        if (form.showAndWait() != null) load();
    }

    private void toggleSelected() {
        MembershipPlan p = selected();
        if (p == null) return;
        try { controller.toggleActive(p.getId()); load(); }
        catch (Exception ex) { Dialogs.error("Failed", ex.getMessage()); }
    }

    private void deleteSelected() {
        MembershipPlan p = selected();
        if (p == null) return;
        if (!Dialogs.confirm("Delete plan",
                "Delete plan \"" + p.getPlanName() + "\"? This cannot be undone.")) return;
        try { controller.delete(p.getId()); load(); }
        catch (Exception ex) { Dialogs.error("Delete failed", ex.getMessage()); }
    }

    // ───────────── Helpers ─────────────

    private ListCell<String> prettyCell() {
        return new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                getStyleClass().remove("pl-list-cell");
                getStyleClass().add("pl-list-cell");
            }
        };
    }

    private TableCell<MembershipPlan, String> badgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item);
                b.getStyleClass().addAll("pl-badge",
                        "ACTIVE".equals(item) ? "pl-badge-green" : "pl-badge-red");
                setGraphic(b);
                setText(null);
            }
        };
    }

    private Button primary(String t, Runnable a)   { return btn(t, "pl-btn-primary", a); }
    private Button secondary(String t, Runnable a) { return btn(t, "pl-btn-secondary", a); }
    private Button danger(String t, Runnable a)    { return btn(t, "pl-btn-danger", a); }

    private Button btn(String t, String styleClass, Runnable a) {
        Button b = new Button(t);
        b.getStyleClass().add(styleClass);
        b.setOnAction(e -> a.run());
        return b;
    }

    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    private <T> TableColumn<MembershipPlan, T> col(String title,
                                                   Function<MembershipPlan, T> getter,
                                                   double width) {
        TableColumn<MembershipPlan, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}