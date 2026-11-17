package com.time.coworking.ui;

import com.time.coworking.Session;
import com.time.coworking.controller.LoginController;
import com.time.coworking.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main application shell: sidebar + topbar + content area.
 * Styling lives in /css/shell.css (same emerald palette as login.css).
 */
public class MainView {

    private final Stage stage;
    private final LoginController loginController = new LoginController();

    private final StackPane contentArea = new StackPane();
    private final VBox sidebar = new VBox(4);
    private final Label userLabel = new Label();
    private final Label pageTitle = new Label("Dashboard");
    private Button activeNavButton;
    private Button dashboardButton;

    public MainView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        buildSidebar();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("shell-root");
        root.setLeft(sidebar);
        root.setTop(createTopbar());
        root.setCenter(contentArea);
        contentArea.getStyleClass().add("content-area");

        Scene scene = new Scene(root, 1280, 800);
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception ignored) {
            // main.css is optional
        }
        scene.getStylesheets().add(
                getClass().getResource("/css/shell.css").toExternalForm());

        stage.setTitle("TIME Co-working — Management System");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.centerOnScreen();
        stage.show();

        // Land on Dashboard by default
        showView("Dashboard", new DashboardView().build());
        setActive(dashboardButton);
    }

    // ───────────────────────── Sidebar ─────────────────────────

    private void buildSidebar() {
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);
        sidebar.setMinWidth(250);
        sidebar.setMaxWidth(250);

        // Logo + name (same as login brand panel)
        Label logoText = new Label("T");
        logoText.getStyleClass().add("brand-logo-text");
        StackPane logo = new StackPane(logoText);
        logo.getStyleClass().add("brand-logo");
        Label name = new Label("TIME Co-working");
        name.getStyleClass().add("brand-name");
        HBox brand = new HBox(12, logo, name);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("sidebar-brand");

        Label menuLabel = new Label("MENU");
        menuLabel.getStyleClass().add("sidebar-section");

        sidebar.getChildren().clear();
        sidebar.getChildren().addAll(brand, menuLabel);

        dashboardButton = addNav("Dashboard", () -> showView("Dashboard", new DashboardView().build()));
        addNav("Members",              () -> showView("Members",              new MembersView().build()));
        addNav("Membership Plans",     () -> showView("Membership Plans",     new PlansView().build()));
        addNav("Workspaces",           () -> showView("Workspaces",           new WorkspacesView().build()));
        addNav("Bookings",             () -> showView("Bookings",             new BookingsView().build()));
        addNav("Check-in / Check-out", () -> showView("Check-in / Check-out", new CheckInView().build()));
        addNav("Payments",             () -> showView("Payments",             new PaymentsView().build()));
        addNav("Invoices",             () -> showView("Invoices",             new InvoicesView().build()));
        addNav("Reports",              () -> showView("Reports",              new ReportsView().build()));

        if (Session.isAdmin()) {
            sidebar.getChildren().add(new Separator());
            Label adminLabel = new Label("ADMIN");
            adminLabel.getStyleClass().add("sidebar-section");
            sidebar.getChildren().add(adminLabel);
            addNav("Audit Log", () -> showView("Audit Log", new AuditLogView().build()));
        }

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        Label footer = new Label("© 2026 TIME. All rights reserved.");
        footer.getStyleClass().add("brand-footer");
        sidebar.getChildren().addAll(grow, footer);
    }

    private Button addNav(String label, Runnable action) {
        Button b = new Button(label);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setOnAction(e -> {
            action.run();
            setActive(b);
        });
        sidebar.getChildren().add(b);
        return b;
    }

    private void setActive(Button b) {
        if (activeNavButton != null) activeNavButton.getStyleClass().remove("nav-active");
        activeNavButton = b;
        if (!b.getStyleClass().contains("nav-active")) b.getStyleClass().add("nav-active");
    }

    // ───────────────────────── Topbar ─────────────────────────

    private HBox createTopbar() {
        User u = Session.getUser();
        userLabel.setText((u == null ? "" : u.getFullName())
                + "  ·  " + (u == null ? "" : u.getRole()));
        userLabel.getStyleClass().add("user-chip");

        pageTitle.getStyleClass().add("topbar-title");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("logout-button");
        logoutBtn.setOnAction(e -> {
            loginController.logout();
            new LoginView(stage).show();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topbar = new HBox(12, pageTitle, spacer, userLabel, logoutBtn);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getStyleClass().add("topbar");
        return topbar;
    }

    // ───────────────────────── Content ─────────────────────────

    private void showView(String title, Region view) {
        pageTitle.setText(title);

        StackPane holder = new StackPane(view);
        holder.setPadding(new Insets(24));

        ScrollPane scroll = new ScrollPane(holder);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("content-scroll");

        contentArea.getChildren().setAll(scroll);
    }
}