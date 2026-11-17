package com.time.coworking.ui;

import com.time.coworking.Session;
import com.time.coworking.controller.MemberController;
import com.time.coworking.enums.MemberStatus;
import com.time.coworking.model.Member;
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

import java.util.function.Function;

/**
 * Members screen — same emerald theme + hero + stats + toolbar + table layout
 * as WorkspacesView. Styling in /css/members.css (mb-* classes).
 */
public class MembersView {

    private final MemberController controller = new MemberController();
    private final ObservableList<Member> data = FXCollections.observableArrayList();

    private TableView<Member> table;
    private TextField searchField;
    private ComboBox<String> statusFilter;
    private final Label totalVal   = new Label("0");
    private final Label activeVal  = new Label("0");
    private final Label expiredVal = new Label("0");

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("mb-root");
        root.setPadding(new Insets(0));

        // Load members.css
        String css = getClass().getResource("/css/members.css").toExternalForm();
        root.getStylesheets().add(css);
        root.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null && !sc.getStylesheets().contains(css)) sc.getStylesheets().add(css);
        });

        StackPane hero = buildHero();

        // ── Toolbar ──
        searchField = new TextField();
        searchField.setPromptText("Search by name, email, phone, company…");
        searchField.setPrefWidth(320);
        searchField.getStyleClass().add("mb-search");
        searchField.textProperty().addListener((o, a, b) -> load());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "ACTIVE", "EXPIRED", "SUSPENDED", "CANCELLED");
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(180);
        statusFilter.getStyleClass().add("mb-combo");
        statusFilter.setCellFactory(lv -> prettyCell());
        statusFilter.setButtonCell(prettyCell());
        statusFilter.setOnAction(e -> load());

        Button add     = primary("Add Member", () -> openForm(null));
        Button edit    = secondary("Edit",      () -> openForm(selected()));
        Button renew   = secondary("Renew",     this::renewSelected);
        Button delete  = danger("Delete",       this::deleteSelected);
        Button refresh = secondary("Refresh",   this::load);

        if (!Session.isAdmin()) delete.setDisable(true);

        Label statusLbl = new Label("STATUS");
        statusLbl.getStyleClass().add("mb-toolbar-label");

        HBox toolbar = new HBox(10,
                searchField, statusLbl, statusFilter,
                spacer(), add, edit, renew, delete, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("mb-toolbar");

        // ── Table ──
        table = new TableView<>(data);
        table.getStyleClass().add("mb-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        Label empty = new Label("No members found");
        empty.getStyleClass().add("mb-empty");
        table.setPlaceholder(empty);
        table.setRowFactory(tv -> {
            TableRow<Member> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openForm(row.getItem());
            });
            return row;
        });

        TableColumn<Member, String> statusCol = col("Status", m -> m.getStatus().name(), 130);
        statusCol.setCellFactory(c -> badgeCell(true));

        TableColumn<Member, String> typeCol = col("Type", m -> m.getMembershipType(), 160);
        typeCol.setCellFactory(c -> typeBadgeCell());

        table.getColumns().addAll(
                col("ID",           m -> m.getId(),                                    70),
                col("Full Name",    m -> m.getFullName(),                              180),
                col("Email",        m -> m.getEmail(),                                 220),
                col("Phone",        m -> m.getPhone(),                                 140),
                col("Company",      m -> m.getCompany(),                               160),
                typeCol,
                statusCol,
                col("Join Date",    m -> String.valueOf(m.getJoinDate()),               120),
                col("Expiry Date",  m -> String.valueOf(m.getExpiryDate()),             120)
        );
        table.getColumns().get(0).getStyleClass().add("mb-col-id");
        table.getColumns().get(1).getStyleClass().add("mb-col-name");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(table.widthProperty());
        clip.heightProperty().bind(table.heightProperty());
        table.setClip(clip);

        StackPane tableCard = new StackPane(table);
        tableCard.getStyleClass().add("mb-table-card");
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
        Circle c1 = circle(120, 760, -30, "mb-circle");
        Circle c2 = circle(70, 560, 170, "mb-circle");
        Circle c3 = circle(30, 880, 120, "mb-circle-gold");
        Circle c4 = circle(10, 60, 40, "mb-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label eyebrow  = new Label("MEMBER MANAGEMENT");
        eyebrow.getStyleClass().add("mb-hero-eyebrow");
        Label headline = new Label("Members");
        headline.getStyleClass().add("mb-hero-headline");
        Label sub      = new Label("Manage memberships, renewals and contact details");
        sub.getStyleClass().add("mb-hero-subtitle");
        VBox left = new VBox(6, eyebrow, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                stat("SHOWN",   totalVal),
                stat("ACTIVE",  activeVal),
                stat("EXPIRED", expiredVal));
        stats.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, stats);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("mb-hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("mb-hero");
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
        value.getStyleClass().add("mb-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("mb-stat-caption");
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("mb-stat");
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
        String q = searchField.getText();
        MemberStatus st = "All".equals(statusFilter.getValue())
                ? null : MemberStatus.valueOf(statusFilter.getValue());

        Task<java.util.List<Member>> task = new Task<>() {
            @Override protected java.util.List<Member> call() {
                return (q == null || q.isBlank())
                        ? (st == null ? controller.list() : controller.search("", st))
                        : controller.search(q, st);
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
        long active  = data.stream().filter(m -> m.getStatus() == MemberStatus.ACTIVE).count();
        long expired = data.stream().filter(m -> m.getStatus() == MemberStatus.EXPIRED).count();
        totalVal.setText(String.valueOf(data.size()));
        activeVal.setText(String.valueOf(active));
        expiredVal.setText(String.valueOf(expired));
    }

    private Member selected() {
        Member m = table.getSelectionModel().getSelectedItem();
        if (m == null) Dialogs.warn("No selection", "Select a member first.");
        return m;
    }

    private void openForm(Member m) {
        MemberFormView form = new MemberFormView(m);
        if (form.showAndWait() != null) load();
    }

    private void renewSelected() {
        Member m = selected();
        if (m == null) return;

        TextInputDialog dlg = new TextInputDialog("30");
        dlg.setTitle("Renew Membership");
        dlg.setHeaderText("Extend " + m.getFullName() + " by how many days?");
        dlg.setContentText("Days:");
        dlg.showAndWait().ifPresent(days -> {
            try {
                int d = Integer.parseInt(days.trim());
                controller.renew(m.getId(), d);
                load();
            } catch (NumberFormatException ex) {
                Dialogs.error("Invalid input", "Days must be a number.");
            } catch (Exception ex) {
                Dialogs.error("Renew failed", ex.getMessage());
            }
        });
    }

    private void deleteSelected() {
        Member m = selected();
        if (m == null) return;
        if (!Dialogs.confirm("Delete member",
                "Delete " + m.getFullName() + "? This cannot be undone.")) return;
        try { controller.delete(m.getId()); load(); }
        catch (Exception ex) { Dialogs.error("Delete failed", ex.getMessage()); }
    }

    // ───────────── Helpers ─────────────

    private ListCell<String> prettyCell() {
        return new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.replace('_', ' '));
                getStyleClass().remove("mb-list-cell");
                getStyleClass().add("mb-list-cell");
            }
        };
    }

    private TableCell<Member, String> badgeCell(boolean isStatus) {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item.replace('_', ' '));
                b.getStyleClass().addAll("mb-badge", statusClass(item));
                setGraphic(b);
                setText(null);
            }
        };
    }

    private TableCell<Member, String> typeBadgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label b = new Label(item);
                b.getStyleClass().addAll("mb-badge", typeClass(item));
                setGraphic(b);
                setText(null);
            }
        };
    }

    private String statusClass(String s) {
        return switch (s) {
            case "ACTIVE"    -> "mb-badge-green";
            case "EXPIRED"   -> "mb-badge-amber";
            case "SUSPENDED" -> "mb-badge-red";
            case "CANCELLED" -> "mb-badge-slate";
            default          -> "mb-badge-slate";
        };
    }

    private String typeClass(String t) {
        if (t == null) return "mb-badge-slate";
        return switch (t) {
            case "Day Pass"               -> "mb-badge-teal";
            case "Weekly Pass"            -> "mb-badge-teal";
            case "Monthly Hot Desk"       -> "mb-badge-gold";
            case "Dedicated Desk"         -> "mb-badge-dark";
            case "Private Office"         -> "mb-badge-gold";
            case "Startup Package"        -> "mb-badge-dark";
            case "Meeting Room Package"   -> "mb-badge-teal";
            default                       -> "mb-badge-slate";
        };
    }

    private Button primary(String t, Runnable a)   { return btn(t, "mb-btn-primary", a); }
    private Button secondary(String t, Runnable a) { return btn(t, "mb-btn-secondary", a); }
    private Button danger(String t, Runnable a)    { return btn(t, "mb-btn-danger", a); }

    private Button btn(String t, String styleClass, Runnable a) {
        Button b = new Button(t);
        b.getStyleClass().add(styleClass);
        b.setOnAction(e -> a.run());
        return b;
    }

    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }

    private <T> TableColumn<Member, T> col(String title,
                                           Function<Member, T> getter,
                                           double width) {
        TableColumn<Member, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}