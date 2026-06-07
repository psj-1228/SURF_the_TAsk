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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "focus_sessions")
public class FocusSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "actual_finished")
    private Boolean actualFinished;

    protected FocusSession() {
    }

    public FocusSession(User user, Task task, LocalDateTime startAt) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.task = Objects.requireNonNull(task, "task must not be null");
        this.startAt = Objects.requireNonNullElseGet(startAt, LocalDateTime::now);
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        if (startAt == null) {
            startAt = LocalDateTime.now();
        }
    }

    public void finish(boolean actualFinished, LocalDateTime endAt) {
        this.endAt = Objects.requireNonNullElseGet(endAt, LocalDateTime::now);
        this.actualFinished = actualFinished;
        this.active = false;
    }

    public void keepActive() {
        this.endAt = null;
        this.actualFinished = false;
        this.active = true;
    }

    public long getDurationMinutes() {
        LocalDateTime effectiveEnd = endAt == null ? LocalDateTime.now() : endAt;
        return Duration.between(startAt, effectiveEnd).toMinutes();
    }

    public Long getSessionId() {
        return sessionId;
    }

    public User getUser() {
        return user;
    }

    public Task getTask() {
        return task;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public boolean isActive() {
        return active;
    }

    public Boolean getActualFinished() {
        return actualFinished;
    }
}
