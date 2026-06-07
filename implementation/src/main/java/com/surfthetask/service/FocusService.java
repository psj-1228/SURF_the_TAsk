package com.surfthetask.service;

import com.surfthetask.domain.entity.CompletionRecord;
import com.surfthetask.domain.entity.DailyGoal;
import com.surfthetask.domain.entity.FocusSession;
import com.surfthetask.domain.entity.Reminder;
import com.surfthetask.domain.entity.Task;
import com.surfthetask.domain.entity.User;
import com.surfthetask.domain.enums.AlertChannel;
import com.surfthetask.domain.enums.ReminderType;
import com.surfthetask.domain.enums.TaskStatus;
import com.surfthetask.dto.request.FocusFinishReqDto;
import com.surfthetask.exception.BadRequestException;
import com.surfthetask.exception.ForbiddenException;
import com.surfthetask.exception.NotFoundException;
import com.surfthetask.repository.CompletionRecordRepository;
import com.surfthetask.repository.FocusSessionRepository;
import com.surfthetask.repository.ReminderRepository;
import com.surfthetask.repository.TaskRepository;
import com.surfthetask.repository.UserRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FocusService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final ReminderRepository reminderRepository;
    private final CompletionRecordRepository completionRecordRepository;

    public FocusService(
            UserRepository userRepository,
            TaskRepository taskRepository,
            FocusSessionRepository focusSessionRepository,
            ReminderRepository reminderRepository,
            CompletionRecordRepository completionRecordRepository
    ) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.focusSessionRepository = focusSessionRepository;
        this.reminderRepository = reminderRepository;
        this.completionRecordRepository = completionRecordRepository;
    }

    @Transactional
    public FocusSession startFocus(Long userId, Long taskId) {
        User user = findUser(userId);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("task not found: " + taskId));
        if (!task.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("task does not belong to authenticated user");
        }
        if (!task.canStartFocus()) {
            throw new BadRequestException("completed task cannot start focus mode");
        }
        validateSingleActiveSession(userId);

        task.changeStatus(TaskStatus.IN_PROGRESS);
        return focusSessionRepository.save(new FocusSession(user, task, LocalDateTime.now()));
    }

    @Transactional
    public FocusSession finishFocus(Long userId, Long sessionId, FocusFinishReqDto req) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("focus session not found: " + sessionId));

        if (!session.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("focus session does not belong to authenticated user");
        }

        if (!session.isActive()) {
            throw new BadRequestException("focus session is already finished");
        }

        if (req == null || !req.isActualFinished()) {
            session.keepActive();
            scheduleDelayedAlert(session);
            return session;
        }

        session.finish(true, LocalDateTime.now());
        if (req.shouldCompleteTask()) {
            completeFocusedTask(session.getTask());
        } else {
            session.getTask().changeStatus(TaskStatus.TODO);
        }
        return session;
    }

    public void validateSingleActiveSession(Long userId) {
        focusSessionRepository.findByUserUserIdAndActiveTrue(userId)
                .ifPresent(session -> {
                    throw new BadRequestException("active focus session already exists for user");
                });
    }

    @Transactional
    public Reminder scheduleDelayedAlert(FocusSession session) {
        Reminder reminder = new Reminder(
                session.getUser(),
                session.getTask(),
                session,
                ReminderType.DELAYED_IN_SITE,
                AlertChannel.IN_SITE,
                "Focus session is still active. Please confirm whether the task is really finished.",
                LocalDateTime.now().plusMinutes(5)
        );
        return reminderRepository.save(reminder);
    }

    private void completeFocusedTask(Task task) {
        CompletionRecord record = task.markComplete(LocalDateTime.now());
        if (task instanceof DailyGoal dailyGoal) {
            dailyGoal.recordCompletion(record.getCompletedDate());
        }
        completionRecordRepository.save(record);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found: " + userId));
    }
}
