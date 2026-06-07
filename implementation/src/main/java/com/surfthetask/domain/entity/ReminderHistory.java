package com.surfthetask.domain.entity;

import com.surfthetask.domain.enums.ReminderStatus;
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
@Table(name = "reminder_histories")
public class ReminderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_id", nullable = false)
    private Reminder reminder;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private ReminderStatus resultStatus;

    @Column(length = 255)
    private String reason;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    protected ReminderHistory() {
    }

    public ReminderHistory(Reminder reminder, ReminderStatus resultStatus, String reason) {
        this.reminder = Objects.requireNonNull(reminder, "reminder must not be null");
        this.resultStatus = Objects.requireNonNull(resultStatus, "resultStatus must not be null");
        this.reason = reason;
        this.recordedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    public Long getHistoryId() {
        return historyId;
    }

    public Reminder getReminder() {
        return reminder;
    }

    public ReminderStatus getResultStatus() {
        return resultStatus;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
