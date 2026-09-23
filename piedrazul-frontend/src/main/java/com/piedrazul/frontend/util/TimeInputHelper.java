package com.piedrazul.frontend.util;

import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeInputHelper {

    private static final Pattern TIME_PATTERN =
            Pattern.compile("^([01]?\\d|2[0-3]):([0-5]\\d)$");

    private TimeInputHelper() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            return null;
        }

        Matcher matcher = TIME_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return null;
        }

        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        return String.format("%02d:%02d", hour, minute);
    }

    public static boolean isValid(String value) {
        return normalize(value) != null;
    }

    public static boolean isEndAfterStart(String start, String end) {
        String normalizedStart = normalize(start);
        String normalizedEnd = normalize(end);

        if (normalizedStart == null || normalizedEnd == null) {
            return false;
        }

        LocalTime startTime = LocalTime.parse(normalizedStart);
        LocalTime endTime = LocalTime.parse(normalizedEnd);
        return endTime.isAfter(startTime);
    }
}
