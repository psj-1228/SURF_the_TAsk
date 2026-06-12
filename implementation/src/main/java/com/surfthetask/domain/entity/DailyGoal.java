package com.surfthetask.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@DiscriminatorValue("DAILY_GOAL")
public class DailyGoal extends Task {

    @Column(name = "target_count_per_day")
    private int targetCountPerDay;

    @Column(name = "current_streak")
    private int currentStreak;

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

    protected DailyGoal() {
    }

    public DailyGoal(
            User user,
            String title,
            String description,
            int estimatedMinutes,
            int importance,
            int targetCountPerDay
    ) {
        super(user, title, description, estimatedMinutes, importance);
        this.targetCountPerDay = requirePositive(targetCountPerDay, "targetCountPerDay");
        this.currentStreak = 0;
    }

    public void recordCompletion(LocalDate completedDate) {
        Objects.requireNonNull(completedDate, "completedDate must not be null");

        if (completedDate.equals(lastCompletedDate)) {
            return;
        }
        if (lastCompletedDate != null && completedDate.equals(lastCompletedDate.plusDays(1))) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }
        lastCompletedDate = completedDate;
    }

    public void updateTargetCountPerDay(int targetCountPerDay) {
        this.targetCountPerDay = requirePositive(targetCountPerDay, "targetCountPerDay");
    }

    public void resetStreak() {
        currentStreak = 0;
    }

    public boolean isCompletedOn(LocalDate date) {
        return lastCompletedDate != null && lastCompletedDate.equals(date);
    }

    public int getTargetCountPerDay() {
        return targetCountPerDay;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public LocalDate getLastCompletedDate() {
        return lastCompletedDate;
    }

    private static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
