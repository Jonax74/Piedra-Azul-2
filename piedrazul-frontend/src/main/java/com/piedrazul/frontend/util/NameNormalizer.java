package com.piedrazul.frontend.util;

import java.text.Normalizer;

public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            return "";
        }

        return removeAccents(capitalizeWords(trimmed));
    }

    public static String normalizeOrNull(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String removeAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private static String capitalizeWords(String text) {
        StringBuilder result = new StringBuilder(text.length());
        boolean capitalizeNext = true;

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);

            if (Character.isLetter(current)) {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(current));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(current));
                }
            } else {
                result.append(current);
                if (current == ' ' || current == '-' || current == '\'') {
                    capitalizeNext = true;
                }
            }
        }

        return result.toString();
    }
}
