package com.piedrazul.frontend.util;

import java.util.regex.Pattern;

public final class IntegerInputHelper {

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d*");

    private IntegerInputHelper() {
    }

    public static boolean isDigitsOnly(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return DIGITS_ONLY.matcher(value.strip()).matches();
    }

    public static Integer parsePositiveInteger(String value) {
        if (!isDigitsOnly(value)) {
            return null;
        }

        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            int number = Integer.parseInt(trimmed);
            return number > 0 ? number : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Integer parseNonNegativeInteger(String value) {
        if (!isDigitsOnly(value)) {
            return null;
        }

        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
