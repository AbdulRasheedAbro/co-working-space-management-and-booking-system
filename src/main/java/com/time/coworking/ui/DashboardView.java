package com.time.coworking.ui;

import com.time.coworking.Session;
import com.time.coworking.controller.DashboardController;
import com.time.coworking.dto.DashboardStats;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dashboard styled to match the login screen (emerald theme).
 * Styling lives in /css/dashboard.css (same palette as login.css).
 */
public class DashboardView {

    private final DashboardController controller = new DashboardController();

    public Region build() {
        VBox root = new VBox(22);
        root.getStyleClass().add("dash-root");
        root.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

        StackPane hero = buildHero();

        DashboardStats s;
        try {
            s = controller.load();
        } catch (Exception e) {
            Label err = new Label("Could not load dashboard: " + e.getMessage());
            err.getStyleClass().add("dash-error");
            root.getChildren().addAll(hero, err);
            return root;
        }

        // ── KPI grid: 4 equal columns x 2 equal rows ──
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < 2; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(50);
            rc.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rc);
        }

        Node[] cards = {
                kpi("TOTAL MEMBERS", String.valueOf(s.totalMembers()),
                        s.activeMembers() + " active · " + s.expiredMemberships() + " expired",
                        "Members", "tone-emerald"),
                kpi("TODAY'S BOOKINGS", String.valueOf(s.todaysBookings()),
                        s.todaysCheckIns() + " checked in",
                        "Bookings", "tone-dark"),
                kpi("MONTHLY REVENUE", "PKR " + fmt(s.monthlyRevenue()),
                        "Pending: PKR " + fmt(s.pendingPayments()),
                        "Revenue", "tone-teal"),
                kpi("OCCUPANCY RATE", String.format("%.1f%%", s.occupancyRate()),
                        s.occupiedDesks() + " / " + totalDesks(s) + " desks",
                        "Occupancy", "tone-gold"),
                kpi("AVAILABLE DESKS", String.valueOf(s.availableDesks()),
                        "Hot + Dedicated", "Desks", "tone-emerald"),
                kpi("OCCUPIED DESKS", String.valueOf(s.occupiedDesks()),
                        "In use right now", "In use", "tone-dark"),
                kpi("AVAILABLE OFFICES", String.valueOf(s.availableOffices()),
                        "Private offices", "Offices", "tone-teal"),
                kpi("OCCUPIED OFFICES", String.valueOf(s.occupiedOffices()),
                        "Private offices", "In use", "tone-gold")
        };
        for (int i = 0; i < cards.length; i++) {
            grid.add(cards[i], i % 4, i / 4);
        }
        VBox.setVgrow(grid, Priority.ALWAYS);

        root.getChildren().addAll(hero, grid, buildStatusStrip(s));
        playIntro(hero, cards);
        return root;
    }

    // ───────────── Hero banner (same look as login brand panel) ─────────────

    private StackPane buildHero() {
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        Circle c1 = circle(120, 760, -30, "brand-circle");
        Circle c2 = circle(70, 560, 170, "brand-circle");
        Circle c3 = circle(30, 880, 120, "brand-circle-gold");
        Circle c4 = circle(10, 60, 40, "brand-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 25);
        drift(c2, 12, 30, -20);
        drift(c3, 7, -20, -18);
        drift(c4, 6, 20, 15);

        Label greet = new Label(greetingText());
        greet.getStyleClass().add("hero-greeting");

        String userName = Session.getUser() == null ? "" : Session.getUser().getFullName();
        Label headline = new Label("Welcome back, " + userName);
        headline.getStyleClass().add("hero-headline");

        Label sub = new Label("Live snapshot of your co-working space");
        sub.getStyleClass().add("hero-subtitle");

        VBox left = new VBox(6, greet, headline, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        // Live clock chip (same as login)
        Label clock = new Label();
        clock.getStyleClass().add("clock-text");
        Circle liveDot = new Circle(4);
        liveDot.getStyleClass().add("live-dot");
        HBox chip = new HBox(8, liveDot, clock);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("clock-chip");
        chip.setMaxWidth(Region.USE_PREF_SIZE);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy  ·  HH:mm");
        clock.setText(LocalDateTime.now().format(fmt));
        Timeline tick = new Timeline(new KeyFrame(Duration.seconds(15),
                e -> clock.setText(LocalDateTime.now().format(fmt))));
        tick.setCycleCount(Timeline.INDEFINITE);
        tick.play();

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox content = new HBox(left, grow, chip);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("hero-content");

        StackPane hero = new StackPane(decor, content);
        hero.getStyleClass().add("dash-hero");
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

    // ───────────── KPI card ─────────────

    private VBox kpi(String label, String value, String hint, String tagText, String tone) {
        Circle dot = new Circle(7);
        dot.getStyleClass().add("kpi-dot");

        StackPane iconBg = new StackPane(dot);
        iconBg.getStyleClass().add("kpi-icon");
        iconBg.setMinSize(40, 40);
        iconBg.setPrefSize(40, 40);
        iconBg.setMaxSize(40, 40);

        Label l = new Label(label);
        l.getStyleClass().add("kpi-label");

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox topRow = new HBox(12, iconBg, grow, l);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label v = new Label(value);
        v.getStyleClass().add("kpi-value");
        v.setMinWidth(Region.USE_PREF_SIZE);

        Label h = new Label(hint);
        h.getStyleClass().add("kpi-hint");
        h.setWrapText(true);

        Label tag = new Label(tagText);
        tag.getStyleClass().add("kpi-tag");

        Region fill = new Region();
        VBox.setVgrow(fill, Priority.ALWAYS);

        VBox card = new VBox(8, topRow, v, h, fill, tag);
        card.getStyleClass().addAll("kpi-card", tone);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        card.setMinHeight(150);
        GridPane.setHgrow(card, Priority.ALWAYS);
        GridPane.setVgrow(card, Priority.ALWAYS);
        GridPane.setFillWidth(card, true);
        GridPane.setFillHeight(card, true);
        return card;
    }

    // ───────────── Status strip ─────────────

    private HBox buildStatusStrip(DashboardStats s) {
        HBox strip = new HBox(32,
                statusItem("status-dot-live", "Live", "Bookings today: " + s.todaysBookings()),
                statusItem("status-dot-warn", "Expired", s.expiredMemberships() + " memberships"),
                statusItem("status-dot-warn", "Pending", "PKR " + fmt(s.pendingPayments())),
                statusItem("status-dot-live", "Occupancy", String.format("%.1f%%", s.occupancyRate())));
        strip.getStyleClass().add("status-strip");
        strip.setAlignment(Pos.CENTER_LEFT);
        return strip;
    }

    private HBox statusItem(String dotClass, String label, String value) {
        Circle dot = new Circle(4);
        dot.getStyleClass().addAll("status-dot", dotClass);
        Label l = new Label(label);
        l.getStyleClass().add("status-label");
        Label sep = new Label("·");
        sep.getStyleClass().add("status-value");
        Label v = new Label(value);
        v.getStyleClass().add("status-value");
        HBox box = new HBox(8, dot, l, sep, v);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ───────────── Intro animation (staggered like login) ─────────────

    private void playIntro(Node hero, Node[] cards) {
        hero.setOpacity(0);
        hero.setTranslateY(-12);
        FadeTransition hf = new FadeTransition(Duration.millis(420), hero);
        hf.setToValue(1);
        TranslateTransition hs = new TranslateTransition(Duration.millis(420), hero);
        hs.setToY(0);
        hs.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(hf, hs).play();

        for (int i = 0; i < cards.length; i++) {
            Node c = cards[i];
            c.setOpacity(0);
            c.setTranslateY(16);
            FadeTransition f = new FadeTransition(Duration.millis(380), c);
            f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(380), c);
            t.setToY(0);
            t.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition p = new ParallelTransition(f, t);
            PauseTransition delay = new PauseTransition(Duration.millis(150 + i * 70L));
            delay.setOnFinished(e -> p.play());
            delay.play();
        }
    }

    // ───────────── Helpers ─────────────

    private String greetingText() {
        int h = LocalDateTime.now().getHour();
        if (h < 12) return "GOOD MORNING";
        if (h < 17) return "GOOD AFTERNOON";
        return "GOOD EVENING";
    }

    private long totalDesks(DashboardStats s) {
        return s.availableDesks() + s.occupiedDesks();
    }

    private String fmt(BigDecimal b) {
        if (b == null) return "0";
        return String.format("%,.0f", b);
    }
}