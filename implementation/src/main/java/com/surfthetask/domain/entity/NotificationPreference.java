package com.surfthetask.domain.entity;

import com.surfthetask.domain.enums.AlertChannel;
import com.surfthetask.domain.enums.ReminderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_preferences_user", columnNames = "user_id")
)
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "in_site_enabled", nullable = false)
    private boolean inSiteEnabled;

    @Column(name = "availability_reminder_enabled", nullable = false)
    private boolean availabilityReminderEnabled;

    @Column(name = "deadline_reminder_enabled", nullable = false)
    private boolean deadlineReminderEnabled;

    @Column(name = "minimum_interval_minutes", nullable = false)
    private int minimumIntervalMinutes;

    protected NotificationPreference() {
    }

    public NotificationPreference(User user) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        applyDefaults();
    }

    @PrePersist
    protected void onCreate() {
        if (minimumIntervalMinutes <= 0) {
            applyDefaults();
        }
    }

    public boolean canSend(AlertChannel channel, ReminderType type) {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(type, "type must not be null");

        if (channel == AlertChannel.EMAIL && !emailEnabled) {
            return false;
        }
        if (channel == AlertChannel.IN_SITE && !inSiteEnabled) {
            return false;
        }
        if (type == ReminderType.AVAILABILITY_BASED) {
            return availabilityReminderEnabled;
        }
        if (type == ReminderType.DEADLINE_WARNING || type == ReminderType.OVERDUE_ALERT) {
            return deadlineReminderEnabled;
        }
        return true;
    }

    public void update(
            Boolean emailEnabled,
            Boolean inSiteEnabled,
            Boolean availabilityReminderEnabled,
            Boolean deadlineReminderEnabled,
            Integer minimumIntervalMinutes
    ) {
        if (emailEnabled != null) {
            this.emailEnabled = emailEnabled;
        }
        if (inSiteEnabled != null) {
            this.inSiteEnabled = inSiteEnabled;
        }
        if (availabilityReminderEnabled != null) {
            this.availabilityReminderEnabled = availabilityReminderEnabled;
        }
        if (deadlineReminderEnabled != null) {
            this.deadlineReminderEnabled = deadlineReminderEnabled;
        }
        if (minimumIntervalMinutes != null) {
            if (minimumIntervalMinutes <= 0) {
                throw new IllegalArgumentException("minimumIntervalMinutes must be positive");
            }
            this.minimumIntervalMinutes = minimumIntervalMinutes;
        }
    }

    public Long getPreferenceId() {
        return preferenceId;
    }

    public User getUser() {
        return user;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public boolean isInSiteEnabled() {
        return inSiteEnabled;
    }

    public boolean isAvailabilityReminderEnabled() {
        return availabilityReminderEnabled;
    }

    public boolean isDeadlineReminderEnabled() {
        return deadlineReminderEnabled;
    }

    public int getMinimumIntervalMinutes() {
        return minimumIntervalMinutes;
    }

    private void applyDefaults() {
        emailEnabled = true;
        inSiteEnabled = true;
        availabilityReminderEnabled = true;
        deadlineReminderEnabled = true;
        minimumIntervalMinutes = 30;
    }
}
