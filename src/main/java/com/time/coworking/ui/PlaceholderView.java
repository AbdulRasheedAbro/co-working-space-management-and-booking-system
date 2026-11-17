package com.time.coworking.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class PlaceholderView {

    private final String title;
    private final String subtitle;

    public PlaceholderView(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public Region build() {
        Label t = new Label(title);
        t.setFont(Font.font("System", FontWeight.BOLD, 24));
        t.setStyle("-fx-text-fill: #1e3c72;");

        Label s = new Label(subtitle);
        s.setStyle("-fx-text-fill: #7a869a; -fx-font-size: 13px;");
        s.setWrapText(true);
        s.setMaxWidth(560);

        VBox box = new VBox(10, t, s);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        return box;
    }
}