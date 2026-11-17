package com.time.coworking.ui;

import com.time.coworking.controller.LoginController;
import com.time.coworking.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;

/**
 * Login screen (emerald theme): brand panel on the left, sign-in form on the right.
 * Styling lives in /css/login.css — all theme colours are variables at the top
 * of that file, so re-theming takes seconds.
 *
 * Extras: time-based greeting, live date/time chip, "Remember me" (stores the
 * username only), forgot-password hint, animated background, staggered intro.
 * Login runs on a background thread so the window never freezes.
 */
public class LoginView {

    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginView.class);
    private static final String PREF_REMEMBER = "remember";
    private static final String PREF_USERNAME = "username";

    private final Stage stage;
    private final LoginController controller = new LoginController();

    private TextField usernameField;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private Button toggleBtn;
    private Button loginBtn;
    private CheckBox rememberMe;
    private HBox errorBox;
    private Label errorText;
    private VBox formBox;
    private VBox featureBox;
    private boolean passwordVisible = false;

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        HBox root = new HBox(buildBrandPanel(), buildFormPanel());
        root.getStyleClass().add("login-root");

        Scene scene = new Scene(root, 980, 620);
        scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());

        stage.setTitle("TIME Co-working — Sign in");
        stage.setScene(scene);
        stage.setMinWidth(860);
        stage.setMinHeight(600);
        stage.centerOnScreen();
        stage.show();

        loadRememberedUser();
        playIntro();
        (usernameField.getText().isEmpty() ? usernameField : passwordField).requestFocus();
    }

    // ───────────────────────── Left panel ─────────────────────────

    private StackPane buildBrandPanel() {
        // Decorative drifting shapes
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        Circle c1 = circle(190, 360, -50, "brand-circle");
        Circle c2 = circle(120, -30, 600, "brand-circle");
        Circle c3 = circle(46, 330, 430, "brand-circle-gold");
        Circle c4 = circle(14, 70, 150, "brand-circle-gold");
        decor.getChildren().addAll(c1, c2, c3, c4);
        drift(c1, 10, -30, 40);
        drift(c2, 12, 40, -30);
        drift(c3, 7, -20, -25);
        drift(c4, 6, 25, 20);

        // Logo + name
        Label logoText = new Label("T");
        logoText.getStyleClass().add("brand-logo-text");
        StackPane logo = new StackPane(logoText);
        logo.getStyleClass().add("brand-logo");
        Label name = new Label("TIME Co-working");
        name.getStyleClass().add("brand-name");
        HBox top = new HBox(14, logo, name);
        top.setAlignment(Pos.CENTER_LEFT);

        // Live date/time chip
        Label clock = new Label();
        clock.getStyleClass().add("clock-text");
        Circle liveDot = new Circle(4);
        liveDot.getStyleClass().add("live-dot");
        HBox clockChip = new HBox(8, liveDot, clock);
        clockChip.setAlignment(Pos.CENTER_LEFT);
        clockChip.getStyleClass().add("clock-chip");
        clockChip.setMaxWidth(Region.USE_PREF_SIZE);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy  ·  HH:mm");
        clock.setText(LocalDateTime.now().format(fmt));
        Timeline tick = new Timeline(new KeyFrame(Duration.seconds(15),
                e -> clock.setText(LocalDateTime.now().format(fmt))));
        tick.setCycleCount(Timeline.INDEFINITE);
        tick.play();

        Label headline = new Label("Your workspace,\nbeautifully organised.");
        headline.getStyleClass().add("brand-headline");

        Label tagline = new Label("Members, bookings, billing and reports — all in one place.");
        tagline.getStyleClass().add("brand-tagline");
        tagline.setWrapText(true);

        featureBox = new VBox(11,
                feature("Conflict-free workspace booking"),
                feature("Memberships, invoices and payments"),
                feature("Live dashboard and reports"),
                feature("Full audit trail of every action"));

        Region spacerTop = new Region();
        Region spacerBottom = new Region();
        VBox.setVgrow(spacerTop, Priority.ALWAYS);
        VBox.setVgrow(spacerBottom, Priority.ALWAYS);

        Label footer = new Label("© 2026 TIME. All rights reserved.");
        footer.getStyleClass().add("brand-footer");

        VBox content = new VBox(14, top, spacerTop, clockChip, headline, tagline,
                spacer(6), featureBox, spacerBottom, footer);
        content.getStyleClass().add("brand-content");

        StackPane panel = new StackPane(decor, content);
        panel.getStyleClass().add("brand-panel");
        panel.setPrefWidth(420);
        panel.setMinWidth(420);
        panel.setMaxWidth(420);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(panel.widthProperty());
        clip.heightProperty().bind(panel.heightProperty());
        panel.setClip(clip);
        return panel;
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

    private HBox feature(String text) {
        Label check = new Label("✓");
        check.getStyleClass().add("brand-feature-dot");
        check.setMinSize(24, 24);
        check.setPrefSize(24, 24);
        check.setAlignment(Pos.CENTER);

        Label label = new Label(text);
        label.getStyleClass().add("brand-feature");
        label.setMinWidth(Region.USE_PREF_SIZE);   // never truncate with "..."

        HBox row = new HBox(12, check, label);
        row.getStyleClass().add("brand-feature-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ───────────────────────── Right panel ─────────────────────────

    private StackPane buildFormPanel() {
        Label greeting = new Label(greetingText());
        greeting.getStyleClass().add("form-greeting");
        Label title = new Label("Welcome back");
        title.getStyleClass().add("form-title");
        Label subtitle = new Label("Sign in to your account to continue");
        subtitle.getStyleClass().add("form-subtitle");

        // Username
        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().addAll("login-input", "icon-input");
        StackPane userStack = withLeadingIcon(usernameField, "icon-user");

        // Password (+ show/hide)
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().addAll("login-input", "icon-input", "password-input");

        visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("Enter your password");
        visiblePasswordField.getStyleClass().addAll("login-input", "icon-input", "password-input");
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        toggleBtn = new Button("Show");
        toggleBtn.getStyleClass().add("toggle-pass");
        toggleBtn.setFocusTraversable(false);
        toggleBtn.setOnAction(e -> togglePassword());
        StackPane.setAlignment(toggleBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggleBtn, new Insets(0, 8, 0, 0));

        StackPane passStack = withLeadingIcon(passwordField, "icon-lock");
        passStack.getChildren().add(1, visiblePasswordField);
        passStack.getChildren().add(toggleBtn);

        // Remember me + Forgot password
        rememberMe = new CheckBox("Remember me");
        rememberMe.getStyleClass().add("remember");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Button forgot = new Button("Forgot password?");
        forgot.getStyleClass().add("link-small");
        forgot.setFocusTraversable(false);
        forgot.setOnAction(e -> showForgotInfo());
        HBox optionsRow = new HBox(rememberMe, grow, forgot);
        optionsRow.setAlignment(Pos.CENTER_LEFT);

        // Error banner
        Label errIcon = new Label("!");
        errIcon.getStyleClass().add("error-icon");
        errIcon.setMinSize(20, 20);
        errIcon.setAlignment(Pos.CENTER);
        errorText = new Label();
        errorText.getStyleClass().add("error-text");
        errorText.setWrapText(true);
        errorBox = new HBox(10, errIcon, errorText);
        errorBox.setAlignment(Pos.CENTER_LEFT);
        errorBox.getStyleClass().add("error-box");
        errorBox.setVisible(false);
        errorBox.setManaged(false);

        // Buttons
        loginBtn = new Button("Sign in  →");
        loginBtn.getStyleClass().add("primary-button");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> attemptLogin());

        Button exitBtn = new Button("Exit application");
        exitBtn.getStyleClass().add("link-button");
        exitBtn.setCancelButton(true);
        exitBtn.setOnAction(e -> stage.close());
        HBox exitRow = new HBox(exitBtn);
        exitRow.setAlignment(Pos.CENTER);

        Label secure = new Label("🔒  Your connection is protected and every sign-in is logged.");
        secure.getStyleClass().add("secure-note");
        secure.setWrapText(true);

        formBox = new VBox(8,
                greeting, title, subtitle,
                spacer(16),
                fieldLabel("Username"), userStack,
                spacer(6),
                fieldLabel("Password"), passStack,
                spacer(2),
                optionsRow,
                errorBox,
                spacer(6),
                loginBtn,
                exitRow,
                spacer(4),
                secure);
        formBox.setMaxWidth(390);
        formBox.setAlignment(Pos.CENTER_LEFT);

        StackPane panel = new StackPane(formBox);
        panel.getStyleClass().add("form-panel");
        panel.setAlignment(Pos.CENTER);
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    /** Wraps an input in a StackPane and overlays a vector icon on its left side. */
    private StackPane withLeadingIcon(TextField input, String iconClass) {
        Region icon = new Region();
        icon.getStyleClass().addAll("field-icon", iconClass);
        icon.setMinSize(18, 18);
        icon.setPrefSize(18, 18);
        icon.setMaxSize(18, 18);
        icon.setMouseTransparent(true);
        StackPane.setAlignment(icon, Pos.CENTER_LEFT);
        StackPane.setMargin(icon, new Insets(0, 0, 0, 16));
        StackPane stack = new StackPane(input, icon);
        stack.setAlignment(Pos.CENTER_LEFT);
        return stack;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        r.setPrefHeight(h);
        return r;
    }

    private String greetingText() {
        int h = LocalDateTime.now().getHour();
        if (h < 12) return "GOOD MORNING";
        if (h < 17) return "GOOD AFTERNOON";
        return "GOOD EVENING";
    }

    // ───────────────────────── Behaviour ─────────────────────────

    private void loadRememberedUser() {
        if (PREFS.getBoolean(PREF_REMEMBER, false)) {
            usernameField.setText(PREFS.get(PREF_USERNAME, ""));
            rememberMe.setSelected(true);
        }
    }

    private void saveRememberedUser(String username) {
        if (rememberMe.isSelected()) {
            PREFS.putBoolean(PREF_REMEMBER, true);
            PREFS.put(PREF_USERNAME, username);
        } else {
            PREFS.putBoolean(PREF_REMEMBER, false);
            PREFS.remove(PREF_USERNAME);
        }
    }

    private void showForgotInfo() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Forgot password");
        a.setHeaderText("Need help signing in?");
        a.setContentText("Please contact your system administrator to reset your password.");
        a.initOwner(stage);
        a.showAndWait();
    }

    private void togglePassword() {
        passwordVisible = !passwordVisible;
        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
        toggleBtn.setText(passwordVisible ? "Hide" : "Show");
        TextField active = passwordVisible ? visiblePasswordField : passwordField;
        active.requestFocus();
        active.end();
    }

    private void attemptLogin() {
        hideError();
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            (username.isEmpty() ? usernameField : passwordField).requestFocus();
            shake();
            return;
        }

        setLoading(true);

        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return controller.login(username, password);
            }
        };

        task.setOnSucceeded(e -> {
            saveRememberedUser(username);
            // Hook: keep task.getValue() in a session holder here if you need the logged-in user later.
            new MainView(stage).show();
        });

        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            if (ex instanceof IllegalArgumentException) {
                showError(ex.getMessage());
            } else {
                showError("Could not sign in. Please check the database connection and try again.");
                if (ex != null) ex.printStackTrace();
            }
            passwordField.clear();
            (passwordVisible ? visiblePasswordField : passwordField).requestFocus();
            shake();
        });

        Thread t = new Thread(task, "login-task");
        t.setDaemon(true);
        t.start();
    }

    private void setLoading(boolean loading) {
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        visiblePasswordField.setDisable(loading);
        toggleBtn.setDisable(loading);
        rememberMe.setDisable(loading);
        loginBtn.setDisable(loading);
        if (loading) {
            ProgressIndicator pi = new ProgressIndicator();
            pi.setPrefSize(18, 18);
            pi.setStyle("-fx-progress-color: white;");
            loginBtn.setGraphic(pi);
            loginBtn.setText("Signing in…");
        } else {
            loginBtn.setGraphic(null);
            loginBtn.setText("Sign in  →");
        }
    }

    private void showError(String message) {
        errorText.setText(message);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
    }

    private void hideError() {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }

    /** Form fades/slides in, then the feature rows appear one after another. */
    private void playIntro() {
        formBox.setOpacity(0);
        formBox.setTranslateY(18);
        FadeTransition fade = new FadeTransition(Duration.millis(450), formBox);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(450), formBox);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();

        int i = 0;
        for (Node row : featureBox.getChildren()) {
            row.setOpacity(0);
            row.setTranslateX(-24);
            FadeTransition f = new FadeTransition(Duration.millis(420), row);
            f.setToValue(1);
            TranslateTransition s = new TranslateTransition(Duration.millis(420), row);
            s.setToX(0);
            s.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition p = new ParallelTransition(f, s);
            PauseTransition delay = new PauseTransition(Duration.millis(250 + i * 130L));
            delay.setOnFinished(e -> p.play());
            delay.play();
            i++;
        }
    }

    /** Small horizontal shake to signal a problem. */
    private void shake() {
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(0),   new KeyValue(formBox.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(60),  new KeyValue(formBox.translateXProperty(), -9)),
                new KeyFrame(Duration.millis(120), new KeyValue(formBox.translateXProperty(), 9)),
                new KeyFrame(Duration.millis(180), new KeyValue(formBox.translateXProperty(), -6)),
                new KeyFrame(Duration.millis(240), new KeyValue(formBox.translateXProperty(), 6)),
                new KeyFrame(Duration.millis(300), new KeyValue(formBox.translateXProperty(), 0)));
        t.play();
    }
}