package com.time.coworking.ui;

import com.time.coworking.controller.MemberController;
import com.time.coworking.enums.MemberStatus;
import com.time.coworking.model.Member;
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

import java.time.LocalDate;

/**
 * Add / Edit member dialog — mirrors WorkspaceFormView layout and theme.
 * Styled by /css/dialog.css.
 */
public class MemberFormView {

    private final MemberController controller = new MemberController();
    private final Member editing;
    private Member saved;

    public MemberFormView(Member editing) {
        this.editing = editing;
    }

    public Member showAndWait() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(editing == null ? "Add Member" : "Edit Member");
        dialog.setResizable(false);

        // ── Inputs ──
        TextField name    = new TextField(editing == null ? "" : editing.getFullName());
        TextField email   = new TextField(editing == null ? "" : editing.getEmail());
        TextField phone   = new TextField(editing == null ? "" : editing.getPhone());
        TextField cnic    = new TextField(editing == null ? "" : editing.getCnic());
        TextField company = new TextField(editing == null ? "" : editing.getCompany());

        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll(
                "Day Pass", "Weekly Pass", "Monthly Hot Desk", "Dedicated Desk",
                "Private Office", "Startup Package", "Meeting Room Package");
        type.setValue(editing == null ? "Monthly Hot Desk"
                : (editing.getMembershipType() == null
                ? "Monthly Hot Desk"
                : editing.getMembershipType()));

        DatePicker join = new DatePicker(editing == null
                ? LocalDate.now()
                : (editing.getJoinDate() != null ? editing.getJoinDate() : LocalDate.now()));

        DatePicker expiry = new DatePicker(editing == null
                ? LocalDate.now().plusDays(30)
                : (editing.getExpiryDate() != null
                ? editing.getExpiryDate()
                : LocalDate.now().plusDays(30)));

        ComboBox<MemberStatus> status = new ComboBox<>();
        status.getItems().addAll(MemberStatus.values());
        status.setValue(editing == null ? MemberStatus.ACTIVE : editing.getStatus());

        name.setPromptText("e.g. Ali Ahmed");
        email.setPromptText("e.g. ali@example.com");
        phone.setPromptText("+923001234567");
        cnic.setPromptText("35202-1234567-1");
        company.setPromptText("ABC Technologies");

        for (Control c : new Control[]{
                name, email, phone, cnic, company, type, join, expiry, status}) {
            c.getStyleClass().add("dlg-input");
            c.setMaxWidth(Double.MAX_VALUE);
        }

        // ── Header (same as workspace dialog) ──
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

        Label title    = new Label(editing == null ? "Add Member" : "Edit Member");
        title.getStyleClass().add("dlg-title");
        Label subtitle = new Label(editing == null
                ? "Register a new co-working member"
                : "Update this member's details");
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

        // ── Form grid (2 columns) ──
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

        g.add(field("Full Name *",     name),    0, 0);
        g.add(field("Email *",         email),   1, 0);
        g.add(field("Phone *",         phone),   0, 1);
        g.add(field("CNIC *",          cnic),    1, 1);
        g.add(field("Company",         company), 0, 2);
        g.add(field("Membership Type", type),    1, 2);
        g.add(field("Join Date",       join),    0, 3);
        g.add(field("Expiry Date",     expiry),  1, 3);
        g.add(field("Status",          status),  0, 4);

        // ── Buttons ──
        Button save = new Button("Save");
        save.getStyleClass().add("primary-button");
        save.setDefaultButton(true);
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> dialog.close());

        // ── Save logic ──
        save.setOnAction(e -> {
            try {
                Member m = editing == null ? new Member() : editing;
                m.setFullName(name.getText());
                m.setEmail(email.getText());
                m.setPhone(phone.getText());
                m.setCnic(cnic.getText());
                m.setCompany(company.getText());
                m.setMembershipType(type.getValue());
                m.setJoinDate(join.getValue());
                m.setExpiryDate(expiry.getValue());
                m.setStatus(status.getValue());

                saved = editing == null ? controller.add(m) : controller.update(m);
                dialog.close();
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
        root.setPrefWidth(580);

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