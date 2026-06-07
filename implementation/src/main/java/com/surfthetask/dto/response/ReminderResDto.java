package com.surfthetask.dto.response;

import com.surfthetask.domain.entity.Reminder;
import com.surfthetask.domain.enums.AlertChannel;
import com.surfthetask.domain.enums.ReminderStatus;
import com.surfthetask.domain.enums.ReminderType;
import java.time.LocalDateTime;

public record ReminderResDto(
        Long reminderId,
        Long userId,
        Long taskId,
        Long focusSessionId,
        ReminderType reminderType,
        AlertChannel channel,
        String message,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        ReminderStatus status,
        String resultReason
) {

    public static ReminderResDto from(Reminder reminder) {
        Long taskId = reminder.getTask() == null ? null : reminder.getTask().getTaskId();
        Long sessionId = reminder.getFocusSession() == null ? null : reminder.getFocusSession().getSessionId();
        return new ReminderResDto(
                reminder.getReminderId(),
                reminder.getUser().getUserId(),
                taskId,
                sessionId,
                reminder.getReminderType(),
                reminder.getChannel(),
                reminder.getMessage(),
                reminder.getScheduledAt(),
                reminder.getSentAt(),
                reminder.getStatus(),
                reminder.getResultReason()
        );
    }
}
