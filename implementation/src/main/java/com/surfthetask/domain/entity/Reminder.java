package com.surfthetask.domain.entity;

import com.surfthetask.domain.enums.AlertChannel;
import com.surfthetask.domain.enums.ReminderStatus;
import com.surfthetask.domain.enums.ReminderType;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id")
    private Long reminderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "focus_session_id")
    private FocusSession focusSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 40)
    private ReminderType reminderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertChannel channel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderStatus status;

    @Column(name = "result_reason", length = 255)
    private String resultReason;

    protected Reminder() {
    }

    public Reminder(
            User user,
            Task task,
            FocusSession focusSession,
            ReminderType reminderType,
            AlertChannel channel,
            String message,
            LocalDateTime scheduledAt
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.task = task;
        this.focusSession = focusSession;
        this.reminderType = Objects.requireNonNull(reminderType, "reminderType must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.message = requireText(message, "message");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        this.status = ReminderStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = ReminderStatus.PENDING;
        }
    }

    public boolean isDue(LocalDateTime now) {
        return status == ReminderStatus.PENDING
                && !scheduledAt.isAfter(Objects.requireNonNull(now, "now must not be null"));
    }

    public void markSent(LocalDateTime sentAt) {
        this.sentAt = Objects.requireNonNullElseGet(sentAt, LocalDateTime::now);
        this.status = ReminderStatus.SENT;
        this.resultReason = null;
    }

    public void markFailed(String reason) {
        this.status = ReminderStatus.FAILED;
        this.resultReason = reason;
    }

    public void markSkipped(String reason) {
        this.status = ReminderStatus.SKIPPED;
        this.resultReason = reason;
    }

    public void cancel() {
        this.status = ReminderStatus.CANCELED;
    }

    public Long getReminderId() {
        return reminderId;
    }

    public User getUser() {
        return user;
    }

    public Task getTask() {
        return task;
    }

    public FocusSession getFocusSession() {
        return focusSession;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public AlertChannel getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public ReminderStatus getStatus() {
        return status;
    }

    public String getResultReason() {
        return resultReason;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
