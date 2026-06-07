package com.surfthetask.dto.response;

import com.surfthetask.domain.entity.DailyGoal;
import com.surfthetask.domain.entity.DeadlineTask;
import com.surfthetask.domain.entity.Task;
import com.surfthetask.domain.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResDto(
        Long taskId,
        Long userId,
        String taskType,
        String title,
        String description,
        Integer estimatedMinutes,
        Integer importance,
        TaskStatus status,
        LocalDateTime deadlineAt,
        Integer warningThresholdHours,
        Integer targetCountPerDay,
        Integer currentStreak,
        LocalDate lastCompletedDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TaskResDto from(Task task) {
        LocalDateTime deadlineAt = null;
        Integer warningThresholdHours = null;
        Integer targetCountPerDay = null;
        Integer currentStreak = null;
        LocalDate lastCompletedDate = null;
        String taskType = "TASK";

        if (task instanceof DailyGoal dailyGoal) {
            taskType = "DAILY_GOAL";
            targetCountPerDay = dailyGoal.getTargetCountPerDay();
            currentStreak = dailyGoal.getCurrentStreak();
            lastCompletedDate = dailyGoal.getLastCompletedDate();
        }
        if (task instanceof DeadlineTask deadlineTask) {
            taskType = "DEADLINE_TASK";
            deadlineAt = deadlineTask.getDeadlineAt();
            warningThresholdHours = deadlineTask.getWarningThresholdHours();
        }

        return new TaskResDto(
                task.getTaskId(),
                task.getUser().getUserId(),
                taskType,
                task.getTitle(),
                task.getDescription(),
                task.getEstimatedMinutes(),
                task.getImportance(),
                task.getStatus(),
                deadlineAt,
                warningThresholdHours,
                targetCountPerDay,
                currentStreak,
                lastCompletedDate,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
