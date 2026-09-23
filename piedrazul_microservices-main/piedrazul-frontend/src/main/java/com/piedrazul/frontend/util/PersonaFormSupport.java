package com.piedrazul.frontend.util;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class PersonaFormSupport {

    public static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\s'-]+$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d*");

    private PersonaFormSupport() {
    }

    public static void bindNameNormalization(TextField field) {
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) {
                applyNameNormalization(field);
            }
        });
    }

    public static void normalizeNameFields(TextField... fields) {
        for (TextField field : fields) {
            applyNameNormalization(field);
        }
    }

    public static void applyNameNormalization(TextField field) {
        String current = field.getText() == null ? "" : field.getText();
        String normalized = NameNormalizer.normalize(current);
        if (!normalized.equals(current)) {
            field.setText(normalized);
        }
    }

    public static String normalizedName(TextField field) {
        return NameNormalizer.normalize(field.getText());
    }

    public static String normalizedNameOrNull(TextField field) {
        return NameNormalizer.normalizeOrNull(field.getText());
    }

    public static String displayName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return NameNormalizer.normalize(value);
    }

    public static boolean requireName(TextField field, Label errorLabel, String blankMessage) {
        String value = normalizedName(field);
        if (value.isEmpty()) {
            FormFieldHelper.showFieldError(field, errorLabel, blankMessage);
            return false;
        }
        if (!NAME_PATTERN.matcher(value).matches()) {
            FormFieldHelper.showFieldError(field, errorLabel,
                    "Solo letras, espacios, apóstrofes o guiones");
            return false;
        }
        return true;
    }

    public static boolean optionalName(TextField field, Label errorLabel) {
        String value = normalizedName(field);
        if (value.isEmpty()) {
            return true;
        }
        if (!NAME_PATTERN.matcher(value).matches()) {
            FormFieldHelper.showFieldError(field, errorLabel,
                    "Solo letras, espacios, apóstrofes o guiones");
            return false;
        }
        return true;
    }

    public static TextFormatter<String> digitsOnlyFormatter(int maxLength) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (!DIGITS_ONLY.matcher(next).matches() || next.length() > maxLength) {
                return null;
            }
            return change;
        };
        return new TextFormatter<>(filter);
    }

    public static String trim(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    public static String trimOrNull(TextField field) {
        String value = trim(field);
        return value.isEmpty() ? null : value;
    }
}
