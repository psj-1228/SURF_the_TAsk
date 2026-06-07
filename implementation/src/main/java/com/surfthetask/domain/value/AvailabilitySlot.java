package com.surfthetask.domain.value;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class AvailabilitySlot {

    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int durationMinutes;

    public AvailabilitySlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        this.durationMinutes = (int) Duration.between(startTime, endTime).toMinutes();
    }

    public boolean contains(LocalDateTime dateTime) {
        Objects.requireNonNull(dateTime, "dateTime must not be null");
        LocalTime time = dateTime.toLocalTime();
        return dayOfWeek == dateTime.getDayOfWeek()
                && !time.isBefore(startTime)
                && time.isBefore(endTime);
    }

    public boolean isEnoughFor(int estimatedMinutes) {
        return estimatedMinutes > 0 && durationMinutes >= estimatedMinutes;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }
}
