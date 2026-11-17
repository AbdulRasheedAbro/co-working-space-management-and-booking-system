package com.time.coworking.ui;

import com.time.coworking.controller.AuditLogController;
import com.time.coworking.repository.AuditLogRepository.AuditRow;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Audit Log screen — emerald theme, read-only table with filters.
 * Admin-only; sidebar hides the entry for staff.
 */
public class AuditLogView {

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogController controller = new AuditLogController();
    private final ObservableList<AuditRow> data = FXCollections.observableArrayList();

    private TableView<AuditRow> table;
    private TextField usernameField;
    private TextField actionField;
    private ComboBox<String> entityFilter;
    private DatePicker datePicker;

    private final Label shownVal   = new Label("0");
    private final Label usersVal   = new Label("0");
    private final Label actionsVal = new Label("0");

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("aud-root");
        root.setPadding(new Insets(0));

        String css = getClass().getResource("/css/audit.css").toExternalForm();
        root.getStylesheets().add(css);
        root.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null && !sc.getStylesheets().contains(css)) sc.getStylesheets().add(css);
        });

        StackPane hero = buildHero();

        // ── Toolbar ──
        usernameField = new TextField();
        usernameField.setPromptText("Username…");
        usernameField.setPrefWidth(150);
        usernameField.getStyleClass().add("aud-search");

        actionField = new TextField();
        actionField.setPromptText("Action…");
        actionField.setPrefWidth(200);
        actionField.getStyleClass().add("aud-search");

        entityFilter = new ComboBox<>();
        entityFilter.getItems().addAll(
                "All", "User", "Member", "Booking", "Payment",
                "Invoice", "Plan", "Workspace");
        entityFilter.setValue("All");
        entityFilter.setPrefWidth(140);
        entityFilter.getStyleClass().add("aud-search");

        datePicker = new DatePicker();
        datePicker.setPrefWidth(150);
        datePicker.getStyleClass().add("aud-date");

        Button search  = primary("Search", this::load);
        Button clear   = secondary("Clear", this::clearFilters);
        Button refresh = secondary("Refresh", this::load);

        Label userLbl = new Label("USER");
        userLbl.getStyleClass().add("aud-toolbar-label");
        Label actLbl = new Label("ACTION");
        actLbl.getStyleClass().add("aud-toolbar-label");
        Label entLbl = new Label("ENTITY");
        entLbl.getStyleClass().add("aud-toolbar-label");
        Label dtLbl = new Label("DATE");
        dtLbl.getStyleClass().add("aud-toolbar-label");

        HBox toolbar = new HBox(8,
                userLbl, usernameField,
                actLbl, actionField,
                entLbl, entityFilter,
                dtLbl, datePicker,
                spacer(), search, clear, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("aud-toolbar");

        // Enter key triggers search
        usernameField.setOnAction(e -> load());
        actionField.setOnAction(e -> load());

        // ── Table ──
        table = new TableView<>(data);
        table.getStyleClass().add("aud-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        Label empty = new Label("No audit entries match these filters");
        empty.getStyleClass().add("aud-empty");
        table.setPlaceholder(empty);

        TableColumn<AuditRow, String> entityCol = col("Entity", r -> r.entity(), 130);
        entityCol.setCellFactory(c -> entityBadgeCell());

        table.getColumns().addAll(
                col("ID",         r -> r.id(),                                      70),
                col("When",       r -> r.createdAt() == null ? "—"
                        : r.createdAt().format(DT),                                 180),
                col("User",       r -> r.username() == null ? "—" : r.username(),  140),
                col("Action",     r -> r.action(),                                  240),
                entityCol,
                col("Entity ID",  r -> r.entityId() == null ? "—" : r.entityId(),  140),
                col("Details",    r -> r.details() == null ? "—" : r.details(),    250)
        );
        table.getColumns().get(0).getStyleClass().add("aud-col-id");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(table.widthProperty());
        clip.heightProperty().bind(table.heightProperty());
        table.setClip(clip);

        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().add("aud-table-card");
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
        Circle c1 = circle(120, 760, -30, "aud-circle");
        Circle c2 = circle(70, 560, 170, "aud-circle");
        Circle c3 = circle(30, 880, 120, "aud-circle-gold");
        Circle c4 = circle(10, 60, 40, "aud-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label eyebrow  = new Label("AUDIT LOG");
        eyebrow.getStyleClass().add("aud-hero-eyebrow");
        Label headline = new Label("Audit Log");
        headline.getStyleClass().add("aud-hero-headline");
        Label sub      = new Label("Every login, member change, booking, payment and invoice");
        sub.getStyleClass().add("aud-hero-subtitle");
        VBox left = new VBox(6, eyebrow, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                stat("SHOWN",   shownVal),
                stat("USERS",   usersVal),
                stat("ACTIONS", actionsVal));
        stats.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, stats);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("aud-hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("aud-hero");
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
        value.getStyleClass().add("aud-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("aud-stat-caption");
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("aud-stat");
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
        String user = usernameField.getText();
        String action = actionField.getText();
        String entity = "All".equals(entityFilter.getValue()) ? null : entityFilter.getValue();
        LocalDate date = datePicker.getValue();

        Task<List<AuditRow>> task = new Task<>() {
            @Override protected List<AuditRow> call() {
                return controller.search(user, action, entity, date);
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

    private void clearFilters() {
        usernameField.clear();
        actionField.clear();
        entityFilter.setValue("All");
        datePicker.setValue(null);
        load();
    }

    private void updateStats() {
        long distinctUsers = data.stream()
                .map(AuditRow::username)
                .filter(u -> u != null && !u.isBlank())
                .distinct()
                .count();
        long distinctActions = data.stream()
                .map(AuditRow::action)
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .count();
        shownVal.setText(String.valueOf(data.size()));
        usersVal.setText(String.valueOf(distinctUsers));
        actionsVal.setText(String.valueOf(distinctActions));
    }

    // ───────────── Helpers ─────────────

    private TableCell<AuditRow, String> entityBadgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item);
                b.getStyleClass().addAll("aud-badge", entityClass(item));
                setGraphic(b);
                setText(null);
            }
        };
    }

    private String entityClass(String entity) {
        return switch (entity) {
            case "User"      -> "aud-badge-slate";
            case "Member"    -> "aud-badge-teal";
            case "Booking"   -> "aud-badge-green";
            case "Payment"   -> "aud-badge-amber";
            case "Invoice"   -> "aud-badge-amber";
            case "Plan"      -> "aud-badge-teal";
            case "Workspace" -> "aud-badge-green";
            default          -> "aud-badge-slate";
        };
    }

    private Button primary(String t, Runnable a)   { return btn(t, "aud-btn-primary", a); }
    private Button secondary(String t, Runnable a) { return btn(t, "aud-btn-secondary", a); }

    private Button btn(String t, String styleClass, Runnable a) {
        Button b = new Button(t);
        b.getStyleClass().add(styleClass);
        b.setOnAction(e -> a.run());
        return b;
    }

    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    private <T> TableColumn<AuditRow, T> col(String title,
                                             Function<AuditRow, T> getter,
                                             double width) {
        TableColumn<AuditRow, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}