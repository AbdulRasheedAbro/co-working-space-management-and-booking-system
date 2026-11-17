package com.time.coworking.ui;

import com.time.coworking.controller.WorkspaceController;
import com.time.coworking.enums.WorkspaceStatus;
import com.time.coworking.enums.WorkspaceType;
import com.time.coworking.model.*;
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

/** Add / Edit workspace dialog (emerald theme, styled by /css/dialog.css). */
public class WorkspaceFormView {

    private final WorkspaceController controller = new WorkspaceController();
    private final Workspace editing;
    private Workspace saved;

    public WorkspaceFormView(Workspace editing) {
        this.editing = editing;
    }

    public Workspace showAndWait() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(editing == null ? "Add Workspace" : "Edit Workspace");
        dialog.setResizable(false);

        // ── Inputs ──
        ComboBox<WorkspaceType> type = new ComboBox<>();
        type.getItems().addAll(WorkspaceType.values());
        type.setValue(editing == null ? WorkspaceType.HOT_DESK : editing.getType());
        if (editing != null) type.setDisable(true);

        TextField code       = new TextField(editing == null ? "" : editing.getCode());
        TextField floor      = new TextField(editing == null ? "" : editing.getFloor());
        TextField zone       = new TextField(editing == null ? "" : editing.getZone());
        TextField capacity   = new TextField(
                editing == null || editing.getCapacity() == 0 ? "" : String.valueOf(editing.getCapacity()));
        TextField price      = new TextField(
                editing == null || editing.getPrice() == null ? "0" : editing.getPrice().toPlainString());
        TextField facilities = new TextField(editing == null ? "" : editing.getFacilities());

        ComboBox<WorkspaceStatus> status = new ComboBox<>();
        status.getItems().addAll(WorkspaceStatus.values());
        status.setValue(editing == null ? WorkspaceStatus.AVAILABLE : editing.getStatus());

        code.setPromptText("e.g. HD-101");
        floor.setPromptText("e.g. 1st");
        zone.setPromptText("e.g. Zone A");
        capacity.setPromptText("Number of seats");
        price.setPromptText("0");
        facilities.setPromptText("WiFi, AC, Projector…");

        for (Control c : new Control[]{type, code, floor, zone, capacity, price, facilities, status}) {
            c.getStyleClass().add("dlg-input");
            c.setMaxWidth(Double.MAX_VALUE);
        }

        Label priceHint = new Label("Hourly for meeting rooms, monthly for others");
        priceHint.getStyleClass().add("dlg-hint");

        // ── Header (same look as the dashboard hero) ──
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        decor.getChildren().addAll(
                circle(90, 520, -20, "dlg-circle"),
                circle(50, 380, 90, "dlg-circle"),
                circle(22, 470, 70, "dlg-circle-gold"),
                circle(8, 250, 24, "dlg-circle-gold"));

        Label logoText = new Label("T");
        logoText.getStyleClass().add("brand-logo-text");
        StackPane logo = new StackPane(logoText);
        logo.getStyleClass().add("brand-logo");

        Label title = new Label(editing == null ? "Add Workspace" : "Edit Workspace");
        title.getStyleClass().add("dlg-title");
        Label subtitle = new Label(editing == null
                ? "Create a new desk, office or meeting room"
                : "Update the details of this workspace");
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

        // ── Form grid (2 equal columns) ──
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
        g.add(field("Type *", type), 0, 0);
        g.add(field("Code *", code), 1, 0);
        g.add(field("Floor", floor), 0, 1);
        g.add(field("Zone", zone), 1, 1);
        g.add(field("Capacity", capacity), 0, 2);
        g.add(field("Price (PKR) *", price, priceHint), 1, 2);
        Node fac = field("Facilities", facilities);
        g.add(fac, 0, 3);
        GridPane.setColumnSpan(fac, 2);
        g.add(field("Status", status), 0, 4);

        // ── Buttons ──
        Button save = new Button("Save");
        save.getStyleClass().add("primary-button");
        save.setDefaultButton(true);
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> dialog.close());

        // ── Save logic (unchanged) ──
        save.setOnAction(e -> {
            try {
                WorkspaceType t = type.getValue();
                BigDecimal p = new BigDecimal(price.getText().trim());
                int cap = capacity.getText().isBlank() ? 0 : Integer.parseInt(capacity.getText().trim());
                long id = editing == null ? 0 : editing.getId();

                Workspace w;
                if (t == WorkspaceType.HOT_DESK || t == WorkspaceType.DEDICATED_DESK) {
                    w = new Desk(id, code.getText(), floor.getText(), zone.getText(),
                            status.getValue(), null, null, null,
                            t, p, facilities.getText());
                } else if (t == WorkspaceType.OFFICE) {
                    w = new Office(id, code.getText(), floor.getText(), zone.getText(),
                            cap, status.getValue(), null, null, null,
                            p, facilities.getText());
                } else {
                    w = new MeetingRoom(id, code.getText(), floor.getText(), zone.getText(),
                            cap, status.getValue(), p, facilities.getText());
                }

                saved = editing == null ? controller.add(w) : controller.update(w);
                dialog.close();
            } catch (NumberFormatException ex) {
                Dialogs.error("Invalid input", "Price and capacity must be numbers.");
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

        Scene scene = new Scene(root);   // height sizes itself to the content
        try { scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm()); }
        catch (Exception ignored) {}
        scene.getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
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

    /** Label above input (same pattern as the login form). */
    private VBox field(String labelText, Node input, Node... extra) {
        Label l = new Label(labelText);
        l.getStyleClass().add("field-label");
        VBox box = new VBox(6, l, input);
        box.getChildren().addAll(extra);
        return box;
    }
}