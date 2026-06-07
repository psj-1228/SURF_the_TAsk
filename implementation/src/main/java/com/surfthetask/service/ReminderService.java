package com.surfthetask.service;

import com.surfthetask.domain.entity.DeadlineTask;
import com.surfthetask.domain.entity.FocusSession;
import com.surfthetask.domain.entity.NotificationPreference;
import com.surfthetask.domain.entity.Reminder;
import com.surfthetask.domain.entity.ReminderHistory;
import com.surfthetask.domain.entity.Task;
import com.surfthetask.domain.entity.User;
import com.surfthetask.domain.enums.AlertChannel;
import com.surfthetask.domain.enums.ReminderStatus;
import com.surfthetask.domain.enums.ReminderType;
import com.surfthetask.domain.enums.TaskStatus;
import com.surfthetask.domain.value.AvailabilitySlot;
import com.surfthetask.dto.request.NotificationPreferenceReqDto;
import com.surfthetask.exception.NotFoundException;
import com.surfthetask.repository.DeadlineTaskRepository;
import com.surfthetask.repository.NotificationPreferenceRepository;
import com.surfthetask.repository.PersonalScheduleRepository;
import com.surfthetask.repository.ReminderHistoryRepository;
import com.surfthetask.repository.ReminderRepository;
import com.surfthetask.repository.TaskRepository;
import com.surfthetask.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReminderService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DeadlineTaskRepository deadlineTaskRepository;
    private final PersonalScheduleRepository personalScheduleRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ReminderRepository reminderRepository;
    private final ReminderHistoryRepository reminderHistoryRepository;
    private final ScheduleAnalyzer scheduleAnalyzer;
    private final PriorityCalculator priorityCalculator;

    public ReminderService(
            UserRepository userRepository,
            TaskRepository taskRepository,
            DeadlineTaskRepository deadlineTaskRepository,
            PersonalScheduleRepository personalScheduleRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            ReminderRepository reminderRepository,
            ReminderHistoryRepository reminderHistoryRepository,
            ScheduleAnalyzer scheduleAnalyzer,
            PriorityCalculator priorityCalculator
    ) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.deadlineTaskRepository = deadlineTaskRepository;
        this.personalScheduleRepository = personalScheduleRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.reminderRepository = reminderRepository;
        this.reminderHistoryRepository = reminderHistoryRepository;
        this.scheduleAnalyzer = scheduleAnalyzer;
        this.priorityCalculator = priorityCalculator;
    }

    @Transactional
    public Reminder checkAvailabilityReminder(User user, LocalDateTime now) {
        NotificationPreference preference = getOrCreatePreference(user);
        if (!preference.canSend(AlertChannel.EMAIL, ReminderType.AVAILABILITY_BASED)) {
            return null;
        }

        List<AvailabilitySlot> slots = scheduleAnalyzer.calculateAvailability(
                personalScheduleRepository.findByUserUserId(user.getUserId())
        );
        if (!scheduleAnalyzer.isAvailable(now, slots)) {
            return null;
        }

        if (hasRecentUserReminder(user, ReminderType.AVAILABILITY_BASED, AlertChannel.EMAIL, now, preference)) {
            return null;
        }

        List<Task> incompleteTasks = taskRepository.findByUserUserIdAndStatusNotOrderByCreatedAtDesc(
                user.getUserId(),
                TaskStatus.DONE
        );
        if (incompleteTasks.isEmpty()) {
            return null;
        }

        Task topTask = priorityCalculator.sortTasks(incompleteTasks, now).get(0);
        Reminder reminder = reminderRepository.save(new Reminder(
                user,
                topTask,
                null,
                ReminderType.AVAILABILITY_BASED,
                AlertChannel.EMAIL,
                "Available time detected. Recommended task: " + topTask.getTitle(),
                now
        ));
        sendReminder(reminder);
        return reminder;
    }

    @Transactional
    public List<Reminder> checkDeadlineReminder(User user, LocalDateTime now) {
        NotificationPreference preference = getOrCreatePreference(user);
        List<Reminder> created = new ArrayList<>();
        if (!preference.canSend(AlertChannel.EMAIL, ReminderType.DEADLINE_WARNING)) {
            return created;
        }

        List<DeadlineTask> tasks = deadlineTaskRepository.findByUserUserIdAndStatusNot(user.getUserId(), TaskStatus.DONE);
        for (DeadlineTask task : tasks) {
            ReminderType type = null;
            if (task.isOverdue(now)) {
                type = ReminderType.OVERDUE_ALERT;
            } else if (task.isDeadlineNear(now)) {
                type = ReminderType.DEADLINE_WARNING;
            }

            if (type == null || hasRecentTaskReminder(task, type, now, preference)) {
                continue;
            }

            Reminder reminder = reminderRepository.save(new Reminder(
                    user,
                    task,
                    null,
                    type,
                    AlertChannel.EMAIL,
                    deadlineMessage(task, type, now),
                    now
            ));
            sendReminder(reminder);
            created.add(reminder);
        }
        return created;
    }

    @Transactional
    public Reminder checkDelayedInSiteAlert(FocusSession session, LocalDateTime now) {
        if (!session.isActive() || session.getTask().isCompleted()) {
            return null;
        }
        Reminder reminder = new Reminder(
                session.getUser(),
                session.getTask(),
                session,
                ReminderType.DELAYED_IN_SITE,
                AlertChannel.IN_SITE,
                "Focus session is still active. Please return to the task.",
                now
        );
        return reminderRepository.save(reminder);
    }

    @Transactional
    public boolean sendReminder(Reminder reminder) {
        if (reminder.getStatus() != ReminderStatus.PENDING) {
            return false;
        }
        reminder.markSent(LocalDateTime.now());
        reminderHistoryRepository.save(new ReminderHistory(reminder, ReminderStatus.SENT, "sent by local notification stub"));
        return true;
    }

    public List<Reminder> listReminders(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user not found: " + userId);
        }
        return reminderRepository.findByUserUserIdOrderByScheduledAtDesc(userId);
    }

    @Transactional
    public NotificationPreference updatePreference(Long userId, NotificationPreferenceReqDto req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found: " + userId));
        NotificationPreference preference = getOrCreatePreference(user);
        preference.update(
                req.emailEnabled(),
                req.inSiteEnabled(),
                req.availabilityReminderEnabled(),
                req.deadlineReminderEnabled(),
                req.minimumIntervalMinutes()
        );
        return preference;
    }

    @Transactional
    public NotificationPreference getOrCreatePreference(User user) {
        return notificationPreferenceRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> notificationPreferenceRepository.save(new NotificationPreference(user)));
    }

    public List<User> findReminderUsers() {
        return userRepository.findAll();
    }

    public List<Reminder> findDueReminders(LocalDateTime now) {
        return reminderRepository.findByStatusAndScheduledAtLessThanEqual(ReminderStatus.PENDING, now);
    }

    @Transactional
    public void processDueReminders(LocalDateTime now) {
        for (Reminder reminder : findDueReminders(now)) {
            if (reminder.getFocusSession() != null
                    && (!reminder.getFocusSession().isActive() || reminder.getTask().isCompleted())) {
                skipReminder(reminder, "focus session ended before delayed alert");
                continue;
            }
            sendReminder(reminder);
        }
    }

    @Transactional
    public void skipReminder(Reminder reminder, String reason) {
        reminder.markSkipped(reason);
        reminderHistoryRepository.save(new ReminderHistory(reminder, ReminderStatus.SKIPPED, reason));
    }

    private boolean hasRecentUserReminder(
            User user,
            ReminderType type,
            AlertChannel channel,
            LocalDateTime now,
            NotificationPreference preference
    ) {
        LocalDateTime after = now.minusMinutes(preference.getMinimumIntervalMinutes());
        return !reminderRepository
                .findByUserUserIdAndReminderTypeAndChannelAndScheduledAtAfter(user.getUserId(), type, channel, after)
                .isEmpty();
    }

    private boolean hasRecentTaskReminder(
            DeadlineTask task,
            ReminderType type,
            LocalDateTime now,
            NotificationPreference preference
    ) {
        LocalDateTime after = now.minusMinutes(preference.getMinimumIntervalMinutes());
        return !reminderRepository.findByTaskTaskIdAndReminderTypeAndScheduledAtAfter(task.getTaskId(), type, after)
                .isEmpty();
    }

    private String deadlineMessage(DeadlineTask task, ReminderType type, LocalDateTime now) {
        if (type == ReminderType.OVERDUE_ALERT) {
            return "Deadline has passed: " + task.getTitle();
        }
        return "Deadline is near in " + task.getRemainingHours(now) + " hours: " + task.getTitle();
    }
}
