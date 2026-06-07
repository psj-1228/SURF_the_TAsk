package com.surfthetask.domain.entity;

import com.surfthetask.domain.enums.RepeatType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "personal_schedules")
public class PersonalSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false, length = 20)
    private RepeatType repeatType;

    protected PersonalSchedule() {
    }

    public PersonalSchedule(
            User user,
            String title,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            RepeatType repeatType
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.title = requireText(title, "title");
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.repeatType = Objects.requireNonNullElse(repeatType, RepeatType.WEEKLY);
        validateTime();
    }

    public void update(String title, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, RepeatType repeatType) {
        this.title = requireText(title, "title");
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.repeatType = Objects.requireNonNullElse(repeatType, RepeatType.WEEKLY);
        validateTime();
    }

    public void validateTime() {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    public boolean isOverlapped(PersonalSchedule other) {
        Objects.requireNonNull(other, "other must not be null");
        return dayOfWeek == other.dayOfWeek
                && startTime.isBefore(other.endTime)
                && endTime.isAfter(other.startTime);
    }

    public int getDurationMinutes() {
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
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

    public RepeatType getRepeatType() {
        return repeatType;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
