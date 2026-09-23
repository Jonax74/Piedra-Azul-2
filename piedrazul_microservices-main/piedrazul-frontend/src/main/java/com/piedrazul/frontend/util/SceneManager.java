package com.piedrazul.frontend.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class SceneManager {

    private static final double AUTH_MIN_WIDTH = 900;
    private static final double AUTH_MIN_HEIGHT = 620;

    private static final double DASHBOARD_MIN_WIDTH = 1000;
    private static final double DASHBOARD_MIN_HEIGHT = 650;

    private SceneManager() {
    }

    public static void showLogin(String fxmlPath, Node node) {
        showScreen(fxmlPath, node, "Piedrazul - Login", AUTH_MIN_WIDTH, AUTH_MIN_HEIGHT);
    }

    public static void showRegister(String fxmlPath, Node node) {
        showScreen(fxmlPath, node, "Piedrazul - Registro", AUTH_MIN_WIDTH, AUTH_MIN_HEIGHT);
    }

    public static void showDashboard(String fxmlPath, Node node, String title) {
        showScreen(fxmlPath, node, title, DASHBOARD_MIN_WIDTH, DASHBOARD_MIN_HEIGHT);
    }

    public static void switchScene(String fxmlPath, Node node, String title) {
        showScreen(fxmlPath, node, title, DASHBOARD_MIN_WIDTH, DASHBOARD_MIN_HEIGHT);
    }

    public static void configureInitialStage(Stage stage, Parent root, String title) {
        applyStage(stage, root, title, AUTH_MIN_WIDTH, AUTH_MIN_HEIGHT);
    }

    private static void showScreen(
            String fxmlPath,
            Node node,
            String title,
            double minWidth,
            double minHeight
    ) {
        try {
            URL resource = SceneManager.class.getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalStateException("FXML no encontrado: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) node.getScene().getWindow();
            applyStage(stage, root, title, minWidth, minHeight);

        } catch (Exception e) {
            System.err.println("Error cambiando escena: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private static void applyStage(
            Stage stage,
            Parent root,
            String title,
            double minWidth,
            double minHeight
    ) {
        stage.setTitle(title);
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
        stage.setResizable(true);

        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(new Scene(root));
        } else {
            scene.setRoot(root);
        }

        stage.setMaximized(true);

        if (!stage.isShowing()) {
            stage.show();
        }

        stage.toFront();
    }
}
