package com.surfthetask.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@DiscriminatorValue("DEADLINE_TASK")
public class DeadlineTask extends Task {

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    @Column(name = "warning_threshold_hours")
    private int warningThresholdHours;

    protected DeadlineTask() {
    }

    public DeadlineTask(
            User user,
            String title,
            String description,
            int estimatedMinutes,
            int importance,
            LocalDateTime deadlineAt,
            int warningThresholdHours
    ) {
        super(user, title, description, estimatedMinutes, importance);
        this.deadlineAt = Objects.requireNonNull(deadlineAt, "deadlineAt must not be null");
        this.warningThresholdHours = requirePositive(warningThresholdHours, "warningThresholdHours");
    }

    public boolean validateDeadline(LocalDateTime now) {
        return deadlineAt.isAfter(Objects.requireNonNull(now, "now must not be null"));
    }

    public void updateDeadline(LocalDateTime deadlineAt, int warningThresholdHours) {
        this.deadlineAt = Objects.requireNonNull(deadlineAt, "deadlineAt must not be null");
        this.warningThresholdHours = requirePositive(warningThresholdHours, "warningThresholdHours");
    }

    public long getRemainingHours(LocalDateTime now) {
        return Duration.between(Objects.requireNonNull(now, "now must not be null"), deadlineAt).toHours();
    }

    public boolean isDeadlineNear(LocalDateTime now) {
        long remainingHours = getRemainingHours(now);
        return remainingHours >= 0 && remainingHours <= warningThresholdHours;
    }

    public boolean isOverdue(LocalDateTime now) {
        return deadlineAt.isBefore(Objects.requireNonNull(now, "now must not be null"));
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public int getWarningThresholdHours() {
        return warningThresholdHours;
    }

    private static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
