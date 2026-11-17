package com.time.coworking.ui;

import com.time.coworking.Session;
import com.time.coworking.controller.WorkspaceController;
import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.enums.WorkspaceType;
import com.time.coworking.model.Workspace;
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
 * Workspaces screen styled to match the dashboard (emerald theme).
 * Status column now shows WHO reserved the workspace and the full
 * START → END range with times.
 */
public class WorkspacesView {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final WorkspaceController controller = new WorkspaceController();
    private final ObservableList<Workspace> data = FXCollections.observableArrayList();

    private TableView<Workspace> table;
    private ComboBox<String> typeFilter;
    private final Label totalVal    = new Label("0");
    private final Label availVal    = new Label("0");
    private final Label reservedVal = new Label("0");

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("ws-root");
        root.setPadding(new Insets(0));

        String css = getClass().getResource("/css/workspaces.css").toExternalForm();
        root.getStylesheets().add(css);
        root.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null && !sc.getStylesheets().contains(css)) sc.getStylesheets().add(css);
        });

        StackPane hero = buildHero();

        // ── Toolbar ──
        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All", "HOT_DESK", "DEDICATED_DESK", "OFFICE", "MEETING_ROOM");
        typeFilter.setValue("All");
        typeFilter.setPrefWidth(210);
        typeFilter.getStyleClass().add("ws-combo");
        typeFilter.setCellFactory(lv -> prettyCell());
        typeFilter.setButtonCell(prettyCell());
        typeFilter.setOnAction(e -> load());

        Button add       = primary("Add Workspace",      () -> openForm(null));
        Button edit      = secondary("Edit",             () -> openForm(selected()));
        Button markAvail = secondary("Mark Available",   () -> changeStatus(WorkspaceStatus.AVAILABLE));
        Button markMaint = secondary("Mark Maintenance", () -> changeStatus(WorkspaceStatus.MAINTENANCE));
        Button delete    = danger("Delete",              this::deleteSelected);
        Button refresh   = secondary("Refresh",          this::load);

        // ⭐ Hide admin-only actions completely from staff
        boolean isAdmin = Session.isAdmin();

        add.setVisible(isAdmin);
        add.setManaged(isAdmin);

        edit.setVisible(isAdmin);
        edit.setManaged(isAdmin);

        markAvail.setVisible(isAdmin);
        markAvail.setManaged(isAdmin);

        markMaint.setVisible(isAdmin);
        markMaint.setManaged(isAdmin);

        delete.setVisible(isAdmin);
        delete.setManaged(isAdmin);

        Label typeLbl = new Label("TYPE");
        typeLbl.getStyleClass().add("ws-toolbar-label");

        HBox toolbar = new HBox(10, typeLbl, typeFilter,
                spacer(), add, edit, markAvail, markMaint, delete, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("ws-toolbar");

        // ── Table ──
        table = new TableView<>(data);
        table.getStyleClass().add("ws-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(86);   // taller row for 3-line status
        Label empty = new Label("No workspaces found");
        empty.getStyleClass().add("ws-empty");
        table.setPlaceholder(empty);
        table.setRowFactory(tv -> {
            TableRow<Workspace> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                // Ensure only admin can open the edit form via double click
                if (e.getClickCount() == 2 && !row.isEmpty() && Session.isAdmin()) {
                    openForm(row.getItem());
                }
            });
            return row;
        });

        TableColumn<Workspace, String> typeCol   = col("Type",   w -> w.getType().name(),   160);
        TableColumn<Workspace, String> statusCol = col("Status", w -> w.getStatus().name(), 300);
        typeCol.setCellFactory(c -> badgeCell(false));
        statusCol.setCellFactory(c -> statusCell());

        table.getColumns().addAll(
                col("ID",          w -> w.getId(),                    70),
                typeCol,
                col("Code",        w -> w.getCode(),                 120),
                col("Floor",       w -> w.getFloor(),                 80),
                col("Zone",        w -> w.getZone(),                 120),
                col("Capacity",    w -> w.getCapacity() == 0 ? "-" : w.getCapacity(),  90),
                col("Price (PKR)", w -> w.getPrice() == null ? "-" : MONEY.format(w.getPrice()), 120),
                statusCol,
                col("Facilities",  w -> w.getFacilities(),           200)
        );
        table.getColumns().get(0).getStyleClass().add("ws-col-id");
        table.getColumns().get(2).getStyleClass().add("ws-col-code");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(table.widthProperty());
        clip.heightProperty().bind(table.heightProperty());
        table.setClip(clip);

        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().add("ws-table-card");
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
        Circle c1 = circle(120, 760, -30, "ws-circle");
        Circle c2 = circle(70, 560, 170, "ws-circle");
        Circle c3 = circle(30, 880, 120, "ws-circle-gold");
        Circle c4 = circle(10, 60, 40, "ws-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label eyebrow  = new Label("WORKSPACE MANAGEMENT");
        eyebrow.getStyleClass().add("ws-hero-eyebrow");
        Label headline = new Label("Workspaces");
        headline.getStyleClass().add("ws-hero-headline");
        Label sub      = new Label("Desks, offices and meeting rooms — manage availability and pricing");
        sub.getStyleClass().add("ws-hero-subtitle");
        VBox left = new VBox(6, eyebrow, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                stat("SHOWN",    totalVal),
                stat("AVAILABLE", availVal),
                stat("RESERVED", reservedVal));
        stats.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, stats);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("ws-hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("ws-hero");
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
        value.getStyleClass().add("ws-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("ws-stat-caption");
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("ws-stat");
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
        String t = typeFilter.getValue();
        Task<java.util.List<Workspace>> task = new Task<>() {
            @Override protected java.util.List<Workspace> call() {
                return "All".equals(t) ? controller.list()
                        : controller.byType(WorkspaceType.valueOf(t));
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
        long avail    = data.stream().filter(w -> w.getStatus() == WorkspaceStatus.AVAILABLE).count();
        long reserved = data.stream().filter(w -> w.getStatus() == WorkspaceStatus.RESERVED).count();
        totalVal.setText(String.valueOf(data.size()));
        availVal.setText(String.valueOf(avail));
        reservedVal.setText(String.valueOf(reserved));
    }

    private Workspace selected() {
        Workspace w = table.getSelectionModel().getSelectedItem();
        if (w == null) Dialogs.warn("No selection", "Select a workspace first.");
        return w;
    }

    private void openForm(Workspace w) {
        WorkspaceFormView form = new WorkspaceFormView(w);
        if (form.showAndWait() != null) load();
    }

    private void changeStatus(WorkspaceStatus s) {
        Workspace w = selected();
        if (w == null) return;
        try { controller.changeStatus(w.getId(), s); load(); }
        catch (Exception ex) { Dialogs.error("Failed", ex.getMessage()); }
    }

    private void deleteSelected() {
        Workspace w = selected();
        if (w == null) return;
        if (!Dialogs.confirm("Delete workspace",
                "Delete " + w.getCode() + "? This cannot be undone.")) return;
        try { controller.delete(w.getId()); load(); }
        catch (Exception ex) { Dialogs.error("Delete failed", ex.getMessage()); }
    }

    // ───────────── Helpers ─────────────

    private ListCell<String> prettyCell() {
        return new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.replace('_', ' '));
                getStyleClass().remove("ws-list-cell");
                getStyleClass().add("ws-list-cell");
            }
        };
    }

    private TableCell<Workspace, String> badgeCell(boolean isStatus) {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item.replace('_', ' '));
                b.getStyleClass().addAll("ws-badge", isStatus ? statusClass(item) : typeClass(item));
                setGraphic(b);
                setText(null);
            }
        };
    }

    /** Status cell — badge + member name + full start → end range. */
    private TableCell<Workspace, String> statusCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }

                Workspace w = getTableView().getItems().get(getIndex());
                String status = item;

                Label badge = new Label(status.replace('_', ' '));
                badge.getStyleClass().addAll("ws-badge", statusClass(status));

                VBox box = new VBox(3, badge);

                if (w.getReservedBy() != null && !w.getReservedBy().isBlank()) {
                    Label who = new Label("by " + w.getReservedBy());
                    who.setStyle("-fx-text-fill:#0f172a; -fx-font-size:11.5px; -fx-font-weight:700;");
                    who.setWrapText(true);
                    box.getChildren().add(who);
                }

                if (w.getReservedSlot() != null && !w.getReservedSlot().isBlank()) {
                    Label when = new Label(w.getReservedSlot());
                    when.setStyle("-fx-text-fill:#64748b; -fx-font-size:10.5px;");
                    when.setWrapText(true);
                    box.getChildren().add(when);
                }

                setGraphic(box);
                setText(null);
            }
        };
    }

    private String statusClass(String s) {
        return switch (s) {
            case "AVAILABLE"   -> "ws-badge-green";
            case "RESERVED"    -> "ws-badge-amber";
            case "OCCUPIED"    -> "ws-badge-amber";
            case "MAINTENANCE" -> "ws-badge-red";
            case "UNAVAILABLE" -> "ws-badge-slate";
            default            -> "ws-badge-slate";
        };
    }

    private String typeClass(String s) {
        return switch (s) {
            case "HOT_DESK"       -> "ws-badge-teal";
            case "DEDICATED_DESK" -> "ws-badge-dark";
            case "OFFICE"         -> "ws-badge-gold";
            default               -> "ws-badge-slate";
        };
    }

    private Button primary(String t, Runnable a)   { return btn(t, "ws-btn-primary", a); }
    private Button secondary(String t, Runnable a) { return btn(t, "ws-btn-secondary", a); }
    private Button danger(String t, Runnable a)    { return btn(t, "ws-btn-danger", a); }

    private Button btn(String t, String styleClass, Runnable a) {
        Button b = new Button(t);
        b.getStyleClass().add(styleClass);
        b.setOnAction(e -> a.run());
        return b;
    }

    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    private <T> TableColumn<Workspace, T> col(String title,
                                              Function<Workspace, T> getter,
                                              double width) {
        TableColumn<Workspace, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}
