package com.piedrazul.frontend.util;

import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;

public final class FormFieldHelper {

    private FormFieldHelper() {
    }

    public static void showFieldError(Control control, Label errorLabel, String message) {
        showFieldError(control, control, errorLabel, message);
    }

    public static void showFieldError(
            Control control,
            Parent styleTarget,
            Label errorLabel,
            String message
    ) {
        String errorClass = styleTarget instanceof HBox ? "input-container-error" : "input-error";
        if (!styleTarget.getStyleClass().contains(errorClass)) {
            styleTarget.getStyleClass().add(errorClass);
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public static void clearFieldError(Control control, Label errorLabel) {
        clearFieldError(control, control, errorLabel);
    }

    public static void clearFieldError(Control control, Parent styleTarget, Label errorLabel) {
        styleTarget.getStyleClass().removeAll("input-error", "input-container-error");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public static void bindClearOnChange(Control control, Label errorLabel) {
        bindClearOnChange(control, control, errorLabel);
    }

    public static void bindClearOnChange(Control control, Parent styleTarget, Label errorLabel) {
        if (control instanceof TextInputControl textInput) {
            textInput.textProperty().addListener(
                    (obs, oldVal, newVal) -> clearFieldError(control, styleTarget, errorLabel)
            );
        } else if (control instanceof ComboBox<?>) {
            ((ComboBox<?>) control).valueProperty().addListener(
                    (obs, oldVal, newVal) -> clearFieldError(control, styleTarget, errorLabel)
            );
        } else if (control instanceof DatePicker datePicker) {
            datePicker.valueProperty().addListener(
                    (obs, oldVal, newVal) -> clearFieldError(control, styleTarget, errorLabel)
            );
        }
    }
}
