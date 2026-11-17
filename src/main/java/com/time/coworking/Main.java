package com.time.coworking;

import com.time.coworking.repository.Database;
import com.time.coworking.ui.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        new LoginView(stage).show();
    }

    @Override
    public void stop() {
        Database.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}