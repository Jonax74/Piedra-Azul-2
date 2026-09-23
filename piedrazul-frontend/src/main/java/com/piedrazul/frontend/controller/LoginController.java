package com.piedrazul.frontend.controller;

import com.piedrazul.frontend.auth.KeycloakAuthClient;
import com.piedrazul.frontend.session.SessionManager;
import com.piedrazul.frontend.util.FormFieldHelper;
import com.piedrazul.frontend.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private HBox boxUsername;

    @FXML
    private HBox boxPassword;

    @FXML
    private Label errUsername;

    @FXML
    private Label errPassword;

    @FXML
    public void initialize() {
        FormFieldHelper.bindClearOnChange(txtUsername, boxUsername, errUsername);
        FormFieldHelper.bindClearOnChange(txtPassword, boxPassword, errPassword);
    }

    @FXML
    private void handleLogin() {
        clearErrors();

        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();

        boolean hasError = false;

        if (username.isBlank()) {
            FormFieldHelper.showFieldError(txtUsername, boxUsername, errUsername,
                    "Ingrese su usuario");
            hasError = true;
        }

        if (password.isBlank()) {
            FormFieldHelper.showFieldError(txtPassword, boxPassword, errPassword,
                    "Ingrese su contraseña");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        try {
            SessionManager.login(username, password);

            String rol = SessionManager.getPrimaryRole();
            if (rol == null) {
                SessionManager.clear();
                showAlert("Sin rol asignado",
                        "Tu usuario no tiene roles del sistema. Contacta al administrador.",
                        Alert.AlertType.ERROR);
                return;
            }

            redirectByRole(rol);

        } catch (KeycloakAuthClient.AuthException e) {
            if (isCredentialError(e.getMessage())) {
                FormFieldHelper.showFieldError(txtUsername, boxUsername, errUsername,
                        "Usuario o contraseña incorrectos");
                FormFieldHelper.showFieldError(txtPassword, boxPassword, errPassword,
                        "Usuario o contraseña incorrectos");
            } else {
                FormFieldHelper.showFieldError(txtUsername, boxUsername, errUsername,
                        e.getMessage());
            }
        } catch (Exception e) {
            FormFieldHelper.showFieldError(txtUsername, boxUsername, errUsername,
                    "No se pudo conectar. Intente nuevamente.");
        }
    }

    @FXML
    private void onGoRegister() {
        SessionManager.endRegisterFromAdminPanel();
        SceneManager.showRegister(
                "/view/auth_register/register-View.fxml",
                txtUsername
        );
    }

    private boolean isCredentialError(String message) {
        if (message == null) {
            return true;
        }
        String lower = message.toLowerCase();
        return lower.contains("incorrect") || lower.contains("invalid_grant");
    }

    private void clearErrors() {
        FormFieldHelper.clearFieldError(txtUsername, boxUsername, errUsername);
        FormFieldHelper.clearFieldError(txtPassword, boxPassword, errPassword);
    }

    private void redirectByRole(String rol) {
        switch (rol) {
            case "PACIENTE" -> SceneManager.showDashboard(
                    "/view/dashboard/paciente-dashboard.fxml",
                    txtUsername,
                    "PIEDRAZUL - Menu principal"
            );
            case "MEDICO_TERAPISTA" -> SceneManager.showDashboard(
                    "/view/dashboard/medico-dashboard.fxml",
                    txtUsername,
                    "PIEDRAZUL - Menu principal"
            );
            case "ADMINISTRADOR" -> SceneManager.showDashboard(
                    "/view/dashboard/administrador-dashboard.fxml",
                    txtUsername,
                    "PIEDRAZUL - Menu principal"
            );
            case "AGENDADOR" -> SceneManager.showDashboard(
                    "/view/dashboard/agendador-dashboard.fxml",
                    txtUsername,
                    "PIEDRAZUL - Menu principal"
            );
            default -> {
                SessionManager.clear();
                showAlert("Rol no soportado",
                        "El rol '" + rol + "' no tiene una pantalla asignada.",
                        Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
