package com.piedrazul.citas.domain.model;

import java.time.LocalTime;

public class TimeRange {
    private final LocalTime start;
    private final LocalTime end;

    public TimeRange(LocalTime start, LocalTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.start = start;
        this.end = end;
    }

    public boolean contiene(LocalTime time) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    // Getters
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
}