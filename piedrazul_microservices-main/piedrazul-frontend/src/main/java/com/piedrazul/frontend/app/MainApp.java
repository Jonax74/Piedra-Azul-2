package com.piedrazul.frontend.app;

import com.piedrazul.frontend.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/auth_register/loginView.fxml")
        );

        SceneManager.configureInitialStage(stage, loader.load(), "Piedrazul - Login");
    }

    public static void main(String[] args) {
        launch();
    }
}
