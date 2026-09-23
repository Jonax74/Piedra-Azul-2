package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.session.SessionManager;
import com.piedrazul.frontend.util.NotificationBellSupport;
import com.piedrazul.frontend.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class PacienteDashboardController {

    @FXML
    private Label lblBienvenida;

    @FXML
    private StackPane notificationBell;

    @FXML
    private Label lblNotificationBadge;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) {
            SceneManager.showLogin("/view/auth_register/loginView.fxml",lblBienvenida);
        }
        String username = SessionManager.getUsername();
        lblBienvenida.setText("Bienvenido " + username);
        NotificationBellSupport.attach(notificationBell, lblNotificationBadge);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();

        SceneManager.showLogin(
                "/view/auth_register/loginView.fxml",
                lblBienvenida
        );
    }

    // NUEVO MÉTODO PARA AGENDAR CITA
    @FXML
    private void handleAgendarCita() {
        SceneManager.switchScene(
                "/view/dashboard/agendar-cita-autonoma.fxml",
                lblBienvenida,
                "PIEDRAZUL - Agendar Cita"
        );
    }

    @FXML
    private void handleMisCitas() {
        SceneManager.switchScene(
                "/view/dashboard/misCitasPacienteView.fxml",
                lblBienvenida,
                "PIEDRAZUL - Mis Citas"
        );
    }
}