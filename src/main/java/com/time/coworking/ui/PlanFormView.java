package com.time.coworking.ui;

import com.time.coworking.controller.PlanController;
import com.time.coworking.model.MembershipPlan;
import com.time.coworking.util.Dialogs;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;

/**
 * Add / Edit membership plan dialog — mirrors WorkspaceFormView / MemberFormView.
 */
public class PlanFormView {

    private final PlanController controller = new PlanController();
    private final MembershipPlan editing;
    private MembershipPlan saved;

    public PlanFormView(MembershipPlan editing) {
        this.editing = editing;
    }

    public MembershipPlan showAndWait() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(editing == null ? "Add Plan" : "Edit Plan");
        dialog.setResizable(false);

        // ── Inputs ──
        TextField planName   = new TextField(editing == null ? "" : editing.getPlanName());
        TextField duration   = new TextField(editing == null ? "30" : String.valueOf(editing.getDurationDays()));
        TextField price      = new TextField(editing == null ? "0"  : editing.getPrice().toPlainString());
        TextField allowedHrs = new TextField(editing == null ? "0"  : String.valueOf(editing.getAllowedHours()));
        TextField meetingHrs = new TextField(editing == null ? "0"  : String.valueOf(editing.getIncludedMeetingHours()));
        TextArea  description = new TextArea(editing == null ? "" : editing.getDescription());
        description.setPrefRowCount(3);
        description.setWrapText(true);

        CheckBox active = new CheckBox("Active");
        active.setSelected(editing == null || editing.isActive());

        planName.setPromptText("e.g. Monthly Hot Desk");
        duration.setPromptText("days");
        price.setPromptText("0");
        allowedHrs.setPromptText("hours");
        meetingHrs.setPromptText("hours");

        for (Control c : new Control[]{
                planName, duration, price, allowedHrs, meetingHrs, description, active}) {
            c.getStyleClass().add("dlg-input");
            c.setMaxWidth(Double.MAX_VALUE);
        }

        // ── Header ──
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        decor.getChildren().addAll(
                circle(90, 520, -20, "dlg-circle"),
                circle(50, 380, 90,  "dlg-circle"),
                circle(22, 470, 70,  "dlg-circle-gold"),
                circle(8,  250, 24,  "dlg-circle-gold"));

        Label logoText = new Label("T");
        logoText.getStyleClass().add("brand-logo-text");
        StackPane logo = new StackPane(logoText);
        logo.getStyleClass().add("brand-logo");

        Label title    = new Label(editing == null ? "Add Plan" : "Edit Plan");
        title.getStyleClass().add("dlg-title");
        Label subtitle = new Label(editing == null
                ? "Create a new membership plan"
                : "Update this plan's details");
        subtitle.getStyleClass().add("dlg-subtitle");

        HBox headerContent = new HBox(14, logo, new VBox(3, title, subtitle));
        headerContent.setAlignment(Pos.CENTER_LEFT);
        headerContent.getStyleClass().add("dlg-header");

        StackPane header = new StackPane(decor, headerContent);
        header.getStyleClass().add("dlg-header-bg");
        Rectangle headerClip = new Rectangle();
        headerClip.widthProperty().bind(header.widthProperty());
        headerClip.heightProperty().bind(header.heightProperty());
        header.setClip(headerClip);

        // ── Form ──
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(14);
        g.getStyleClass().add("dlg-body");
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cc.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(cc);
        }
        g.add(field("Plan Name *",   planName), 0, 0);
        g.add(field("Duration *",    duration), 1, 0);
        g.add(field("Price (PKR) *", price),    0, 1);
        g.add(field("Allowed Hours", allowedHrs), 1, 1);
        g.add(field("Included Meeting Hours", meetingHrs), 0, 2);

        Node descBox = field("Description", description);
        GridPane.setColumnSpan(descBox, 2);
        g.add(descBox, 0, 3);

        Node activeBox = field("Status", active);
        g.add(activeBox, 0, 4);

        // ── Buttons ──
        Button save = new Button("Save");
        save.getStyleClass().add("primary-button");
        save.setDefaultButton(true);
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> dialog.close());

        save.setOnAction(e -> {
            try {
                MembershipPlan p = editing == null ? new MembershipPlan() : editing;
                p.setPlanName(planName.getText());
                p.setDurationDays(Integer.parseInt(duration.getText().trim()));
                p.setPrice(new BigDecimal(price.getText().trim()));
                p.setAllowedHours(Integer.parseInt(allowedHrs.getText().trim()));
                p.setIncludedMeetingHours(Integer.parseInt(meetingHrs.getText().trim()));
                p.setDescription(description.getText());
                p.setActive(active.isSelected());

                saved = editing == null ? controller.add(p) : controller.update(p);
                dialog.close();
            } catch (NumberFormatException ex) {
                Dialogs.error("Invalid input", "Duration, price and hours must be numbers.");
            } catch (Exception ex) {
                Dialogs.error("Save failed", ex.getMessage());
            }
        });

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox buttons = new HBox(10, grow, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.getStyleClass().add("dlg-footer");

        VBox root = new VBox(header, g, buttons);
        root.getStyleClass().add("dialog-root");
        root.setPrefWidth(600);

        Scene scene = new Scene(root);
        try { scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm()); }
        catch (Exception ignored) {}
        try { scene.getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm()); }
        catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.showAndWait();
        return saved;
    }

    private Circle circle(double r, double x, double y, String styleClass) {
        Circle c = new Circle(r);
        c.setLayoutX(x);
        c.setLayoutY(y);
        c.getStyleClass().add(styleClass);
        return c;
    }

    private VBox field(String labelText, Node input, Node... extra) {
        Label l = new Label(labelText);
        l.getStyleClass().add("field-label");
        VBox box = new VBox(6, l, input);
        box.getChildren().addAll(extra);
        return box;
    }
}
