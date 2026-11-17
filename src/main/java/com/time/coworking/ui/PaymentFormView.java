package com.time.coworking.ui;

import com.time.coworking.controller.MemberController;
import com.time.coworking.controller.PaymentController;
import com.time.coworking.enums.PaymentMethod;
import com.time.coworking.model.Member;
import com.time.coworking.model.Payment;
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
import javafx.util.StringConverter;

import java.math.BigDecimal;

public class PaymentFormView {

    private final PaymentController controller = new PaymentController();
    private final MemberController memberController = new MemberController();
    private Payment saved;

    public Payment showAndWait() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Record Payment");
        dialog.setResizable(false);

        ComboBox<Member> memberBox = new ComboBox<>();
        memberBox.getItems().setAll(memberController.list());
        memberBox.setConverter(new StringConverter<>() {
            @Override public String toString(Member m) {
                return m == null ? "" : m.getFullName() + "  (" + m.getEmail() + ")";
            }
            @Override public Member fromString(String s) { return null; }
        });
        memberBox.setMaxWidth(Double.MAX_VALUE);

        TextField amountField = new TextField();
        amountField.setPromptText("Amount in PKR");

        ComboBox<PaymentMethod> methodBox = new ComboBox<>();
        methodBox.getItems().addAll(PaymentMethod.values());
        methodBox.setValue(PaymentMethod.CASH);
        methodBox.setMaxWidth(Double.MAX_VALUE);

        TextField referenceField = new TextField();
        referenceField.setPromptText("Reference / cheque # (optional)");

        for (Control c : new Control[]{memberBox, amountField, methodBox, referenceField}) {
            c.getStyleClass().add("dlg-input");
            c.setMaxWidth(Double.MAX_VALUE);
        }

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

        Label title    = new Label("Record Payment");
        title.getStyleClass().add("dlg-title");
        Label subtitle = new Label("Record a walk-in or manual payment");
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

        Node memberField = field("Member *", memberBox);
        GridPane.setColumnSpan(memberField, 2);
        g.add(memberField, 0, 0);

        g.add(field("Amount (PKR) *", amountField), 0, 1);
        g.add(field("Method *", methodBox), 1, 1);

        Node refField = field("Reference", referenceField);
        GridPane.setColumnSpan(refField, 2);
        g.add(refField, 0, 2);

        Button save = new Button("Record Payment");
        save.getStyleClass().add("primary-button");
        save.setDefaultButton(true);
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> dialog.close());

        save.setOnAction(e -> {
            Member m = memberBox.getValue();
            if (m == null) {
                Dialogs.warn("Missing member", "Choose a member first.");
                return;
            }
            try {
                BigDecimal amount = new BigDecimal(amountField.getText().trim());
                saved = controller.record(
                        m.getId(), null, amount,
                        methodBox.getValue(),
                        referenceField.getText().isBlank() ? null : referenceField.getText().trim());
                dialog.close();
            } catch (NumberFormatException ex) {
                Dialogs.error("Invalid input", "Amount must be a number.");
            } catch (Exception ex) {
                Dialogs.error("Payment failed", ex.getMessage());
            }
        });

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox buttons = new HBox(10, grow, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.getStyleClass().add("dlg-footer");

        VBox root = new VBox(header, g, buttons);
        root.getStyleClass().add("dialog-root");
        root.setPrefWidth(560);

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

    private VBox field(String labelText, Node input) {
        Label l = new Label(labelText);
        l.getStyleClass().add("field-label");
        return new VBox(6, l, input);
    }
}