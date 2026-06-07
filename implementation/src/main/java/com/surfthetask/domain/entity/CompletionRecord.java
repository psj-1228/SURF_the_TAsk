package com.surfthetask.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "completion_records")
public class CompletionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column(name = "is_canceled", nullable = false)
    private boolean canceled;

    protected CompletionRecord() {
    }

    public CompletionRecord(Task task, LocalDateTime completedAt) {
        this.task = Objects.requireNonNull(task, "task must not be null");
        this.completedAt = Objects.requireNonNullElseGet(completedAt, LocalDateTime::now);
        this.completedDate = this.completedAt.toLocalDate();
        this.canceled = false;
    }

    @PrePersist
    protected void onCreate() {
        if (completedAt == null) {
            completedAt = LocalDateTime.now();
        }
        if (completedDate == null) {
            completedDate = completedAt.toLocalDate();
        }
    }

    public void cancel() {
        canceled = true;
    }

    public void restore() {
        canceled = false;
    }

    public Long getRecordId() {
        return recordId;
    }

    public Task getTask() {
        return task;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
