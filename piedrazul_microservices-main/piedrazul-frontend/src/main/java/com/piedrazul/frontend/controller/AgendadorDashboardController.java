package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.session.SessionManager;
import com.piedrazul.frontend.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AgendadorDashboardController {

    @FXML
    private Label lblBienvenida;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) {
            SceneManager.showLogin("/view/auth_register/loginView.fxml",lblBienvenida);
        }
        String username = SessionManager.getUsername();
        lblBienvenida.setText("Bienvenido " + username);
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();

        SceneManager.showLogin(
                "/view/auth_register/loginView.fxml",
                lblBienvenida
        );
    }

    @FXML
    private void handleHistorialCitas() {
        SceneManager.switchScene(
                "/view/dashboard/historialCitasView.fxml",
                lblBienvenida,
                "Historial"
        );
    }

    @FXML
    private void handleReportes() {
        SceneManager.switchScene(
                "/view/dashboard/reportesView.fxml",
                lblBienvenida,
                "PIEDRAZUL - Reportes"
        );
    }

    @FXML
    private void handleAgendarCita() {
        SessionManager.endAgendarManualComoMedico();
        SceneManager.switchScene(
                "/view/dashboard/agendar-cita-manual.fxml",
                lblBienvenida,
                "PIEDRAZUL - Agendar Cita Manual"
        );
    }

}
