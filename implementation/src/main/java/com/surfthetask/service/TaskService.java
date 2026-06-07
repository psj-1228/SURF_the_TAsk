package com.surfthetask.service;

import com.surfthetask.domain.entity.CompletionRecord;
import com.surfthetask.domain.entity.DailyGoal;
import com.surfthetask.domain.entity.DeadlineTask;
import com.surfthetask.domain.entity.Reminder;
import com.surfthetask.domain.entity.Task;
import com.surfthetask.domain.entity.User;
import com.surfthetask.domain.enums.TaskStatus;
import com.surfthetask.dto.request.CompletionReqDto;
import com.surfthetask.dto.request.DailyGoalCreateReqDto;
import com.surfthetask.dto.request.DeadlineTaskCreateReqDto;
import com.surfthetask.dto.request.TaskUpdateReqDto;
import com.surfthetask.exception.BadRequestException;
import com.surfthetask.exception.ForbiddenException;
import com.surfthetask.exception.NotFoundException;
import com.surfthetask.repository.CompletionRecordRepository;
import com.surfthetask.repository.FocusSessionRepository;
import com.surfthetask.repository.ReminderHistoryRepository;
import com.surfthetask.repository.ReminderRepository;
import com.surfthetask.repository.TaskRepository;
import com.surfthetask.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final CompletionRecordRepository completionRecordRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final ReminderRepository reminderRepository;
    private final ReminderHistoryRepository reminderHistoryRepository;

    public TaskService(
            UserRepository userRepository,
            TaskRepository taskRepository,
            CompletionRecordRepository completionRecordRepository,
            FocusSessionRepository focusSessionRepository,
            ReminderRepository reminderRepository,
            ReminderHistoryRepository reminderHistoryRepository
    ) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.completionRecordRepository = completionRecordRepository;
        this.focusSessionRepository = focusSessionRepository;
        this.reminderRepository = reminderRepository;
        this.reminderHistoryRepository = reminderHistoryRepository;
    }

    @Transactional
    public DailyGoal createDailyGoal(Long userId, DailyGoalCreateReqDto req) {
        User user = findUser(userId);
        DailyGoal goal = new DailyGoal(
                user,
                req.title(),
                req.description(),
                req.estimatedMinutes(),
                req.importance(),
                req.targetCountPerDay()
        );
        return taskRepository.save(goal);
    }

    @Transactional
    public DeadlineTask createDeadlineTask(Long userId, DeadlineTaskCreateReqDto req) {
        User user = findUser(userId);
        DeadlineTask task = new DeadlineTask(
                user,
                req.title(),
                req.description(),
                req.estimatedMinutes(),
                req.importance(),
                req.deadlineAt(),
                req.warningThresholdHours()
        );
        if (!task.validateDeadline(LocalDateTime.now())) {
            throw new BadRequestException("deadlineAt must be in the future");
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(Long userId, Long taskId, TaskUpdateReqDto req) {
        Task task = findTaskForUser(userId, taskId);
        task.updateDetails(req.title(), req.description(), req.estimatedMinutes(), req.importance());

        if (req.status() != null) {
            task.changeStatus(req.status());
        }
        if (task instanceof DailyGoal dailyGoal && req.targetCountPerDay() != null) {
            dailyGoal.updateTargetCountPerDay(req.targetCountPerDay());
        }
        if (task instanceof DeadlineTask deadlineTask) {
            LocalDateTime deadlineAt = req.deadlineAt() == null ? deadlineTask.getDeadlineAt() : req.deadlineAt();
            Integer threshold = req.warningThresholdHours() == null
                    ? deadlineTask.getWarningThresholdHours()
                    : req.warningThresholdHours();
            deadlineTask.updateDeadline(deadlineAt, threshold);
        }
        return task;
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        findTaskForUser(userId, taskId);
        for (Reminder reminder : reminderRepository.findByTaskTaskId(taskId)) {
            reminderHistoryRepository.deleteByReminderReminderId(reminder.getReminderId());
        }
        reminderRepository.deleteByTaskTaskId(taskId);
        focusSessionRepository.deleteByTaskTaskId(taskId);
        completionRecordRepository.deleteByTaskTaskId(taskId);
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public CompletionRecord completeTask(Long userId, Long taskId, CompletionReqDto req) {
        Task task = findTaskForUser(userId, taskId);
        if (req != null && req.shouldCancel()) {
            CompletionRecord record = completionRecordRepository
                    .findTopByTaskTaskIdAndCanceledFalseOrderByCompletedAtDesc(taskId)
                    .orElseThrow(() -> new BadRequestException("no completion record to cancel"));
            record.cancel();
            task.changeStatus(TaskStatus.TODO);
            return record;
        }

        if (task.isCompleted()) {
            throw new BadRequestException("task is already completed");
        }

        CompletionRecord record = task.markComplete(LocalDateTime.now());
        if (task instanceof DailyGoal dailyGoal) {
            dailyGoal.recordCompletion(record.getCompletedDate());
        }
        return completionRecordRepository.save(record);
    }

    public List<Task> getIncompleteTasks(Long userId) {
        findUser(userId);
        return taskRepository.findByUserUserIdAndStatusNotOrderByCreatedAtDesc(userId, TaskStatus.DONE);
    }

    public List<Task> getTasks(Long userId) {
        findUser(userId);
        return taskRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    public Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("task not found: " + taskId));
    }

    private Task findTaskForUser(Long userId, Long taskId) {
        Task task = findTask(taskId);
        if (!task.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("task does not belong to authenticated user");
        }
        return task;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found: " + userId));
    }
}
