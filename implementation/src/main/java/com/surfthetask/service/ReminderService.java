package com.surfthetask.service;

import com.surfthetask.domain.entity.DailyGoal;
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
import com.surfthetask.repository.DailyGoalRepository;
import com.surfthetask.repository.DeadlineTaskRepository;
import com.surfthetask.repository.NotificationPreferenceRepository;
import com.surfthetask.repository.PersonalScheduleRepository;
import com.surfthetask.repository.ReminderHistoryRepository;
import com.surfthetask.repository.ReminderRepository;
import com.surfthetask.repository.TaskRepository;
import com.surfthetask.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReminderService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DeadlineTaskRepository deadlineTaskRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final PersonalScheduleRepository personalScheduleRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ReminderRepository reminderRepository;
    private final ReminderHistoryRepository reminderHistoryRepository;
    private final ScheduleAnalyzer scheduleAnalyzer;
    private final PriorityCalculator priorityCalculator;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean emailDeliveryEnabled;
    private final String emailFrom;

    public ReminderService(
            UserRepository userRepository,
            TaskRepository taskRepository,
            DeadlineTaskRepository deadlineTaskRepository,
            DailyGoalRepository dailyGoalRepository,
            PersonalScheduleRepository personalScheduleRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            ReminderRepository reminderRepository,
            ReminderHistoryRepository reminderHistoryRepository,
            ScheduleAnalyzer scheduleAnalyzer,
            PriorityCalculator priorityCalculator,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.notification.email.enabled:false}") boolean emailDeliveryEnabled,
            @Value("${app.notification.email.from:no-reply@surfthetask.local}") String emailFrom
    ) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.deadlineTaskRepository = deadlineTaskRepository;
        this.dailyGoalRepository = dailyGoalRepository;
        this.personalScheduleRepository = personalScheduleRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.reminderRepository = reminderRepository;
        this.reminderHistoryRepository = reminderHistoryRepository;
        this.scheduleAnalyzer = scheduleAnalyzer;
        this.priorityCalculator = priorityCalculator;
        this.mailSenderProvider = mailSenderProvider;
        this.emailDeliveryEnabled = emailDeliveryEnabled;
        this.emailFrom = emailFrom;
    }

    @Transactional
    public Reminder checkAvailabilityReminder(User user, LocalDateTime now) {
        NotificationPreference preference = getOrCreatePreference(user);
        if (!preference.canSend(AlertChannel.IN_SITE, ReminderType.AVAILABILITY_BASED)) {
            return null;
        }

        List<AvailabilitySlot> slots = scheduleAnalyzer.calculateAvailability(
                personalScheduleRepository.findByUserUserId(user.getUserId())
        );
        if (!scheduleAnalyzer.isAvailable(now, slots)) {
            return null;
        }

        if (hasRecentUserReminder(user, ReminderType.AVAILABILITY_BASED, AlertChannel.IN_SITE, now, preference)) {
            return null;
        }

        List<Task> unfinishedTasks = unfinishedWorkForAvailability(user, now.toLocalDate());
        if (unfinishedTasks.isEmpty()) {
            return null;
        }

        List<Task> sortedTasks = priorityCalculator.sortTasks(unfinishedTasks, now);
        if (sortedTasks.isEmpty()) {
            return null;
        }

        Task topTask = sortedTasks.get(0);
        Reminder reminder = reminderRepository.save(new Reminder(
                user,
                topTask,
                null,
                ReminderType.AVAILABILITY_BASED,
                AlertChannel.IN_SITE,
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
        if (!preference.canSend(AlertChannel.EMAIL, ReminderType.DAILY_GOAL_DAY_END_ONE_HOUR)
                && !preference.canSend(AlertChannel.EMAIL, ReminderType.DEADLINE_ONE_HOUR)) {
            return created;
        }

        created.addAll(createDailyGoalEmailReminders(user, preference, now));
        created.addAll(createDeadlineTaskEmailReminders(user, preference, now));
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
        if (reminder.getChannel() == AlertChannel.EMAIL) {
            return sendEmailReminder(reminder);
        }
        reminder.markSent(LocalDateTime.now());
        reminderHistoryRepository.save(new ReminderHistory(reminder, ReminderStatus.SENT, "sent by in-site notification"));
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

    private List<Task> unfinishedWorkForAvailability(User user, LocalDate today) {
        List<Task> tasks = new ArrayList<>();
        tasks.addAll(deadlineTaskRepository.findByUserUserIdAndStatusNot(user.getUserId(), TaskStatus.DONE));
        for (DailyGoal dailyGoal : dailyGoalRepository.findByUserUserId(user.getUserId())) {
            if (!dailyGoal.isCompletedOn(today)) {
                tasks.add(dailyGoal);
            }
        }
        return tasks;
    }

    private List<Reminder> createDailyGoalEmailReminders(
            User user,
            NotificationPreference preference,
            LocalDateTime now
    ) {
        List<Reminder> reminders = new ArrayList<>();
        LocalDate today = now.toLocalDate();
        LocalDateTime dayEnd = today.atTime(23, 59);
        for (DailyGoal dailyGoal : dailyGoalRepository.findByUserUserId(user.getUserId())) {
            if (dailyGoal.isCompletedOn(today)) {
                continue;
            }
            addIfNotNull(reminders, createEmailReminderIfDue(
                    user,
                    dailyGoal,
                    ReminderType.DAILY_GOAL_DAY_END_ONE_HOUR,
                    "Daily Goal is due in 1 hour: " + dailyGoal.getTitle(),
                    dayEnd.minusHours(1),
                    now,
                    preference
            ));
            addIfNotNull(reminders, createEmailReminderIfDue(
                    user,
                    dailyGoal,
                    ReminderType.DAILY_GOAL_DAY_END_THIRTY_MINUTES,
                    "Daily Goal is due in 30 minutes: " + dailyGoal.getTitle(),
                    dayEnd.minusMinutes(30),
                    now,
                    preference
            ));
        }
        return reminders;
    }

    private List<Reminder> createDeadlineTaskEmailReminders(
            User user,
            NotificationPreference preference,
            LocalDateTime now
    ) {
        List<Reminder> reminders = new ArrayList<>();
        List<DeadlineTask> tasks = deadlineTaskRepository.findByUserUserIdAndStatusNot(user.getUserId(), TaskStatus.DONE);
        for (DeadlineTask task : tasks) {
            LocalDateTime deadlineAt = task.getDeadlineAt();
            addIfNotNull(reminders, createEmailReminderIfDue(
                    user,
                    task,
                    ReminderType.DEADLINE_ONE_HOUR,
                    "Deadline is due in 1 hour: " + task.getTitle(),
                    deadlineAt.minusHours(1),
                    now,
                    preference
            ));
            addIfNotNull(reminders, createEmailReminderIfDue(
                    user,
                    task,
                    ReminderType.DEADLINE_THIRTY_MINUTES,
                    "Deadline is due in 30 minutes: " + task.getTitle(),
                    deadlineAt.minusMinutes(30),
                    now,
                    preference
            ));
        }
        return reminders;
    }

    private Reminder createEmailReminderIfDue(
            User user,
            Task task,
            ReminderType type,
            String message,
            LocalDateTime targetTime,
            LocalDateTime now,
            NotificationPreference preference
    ) {
        if (!preference.canSend(AlertChannel.EMAIL, type)
                || !isInTargetWindow(now, targetTime)
                || hasTaskReminderAt(task, type, targetTime)) {
            return null;
        }

        Reminder reminder = reminderRepository.save(new Reminder(
                user,
                task,
                null,
                type,
                AlertChannel.EMAIL,
                message,
                targetTime
        ));
        sendReminder(reminder);
        return reminder;
    }

    private boolean sendEmailReminder(Reminder reminder) {
        if (!emailDeliveryEnabled) {
            skipReminder(reminder, "email delivery disabled");
            return false;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return failReminder(reminder, "email sender is not configured");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(reminder.getUser().getEmail());
            message.setSubject(emailSubject(reminder));
            message.setText(reminder.getMessage());
            mailSender.send(message);
            reminder.markSent(LocalDateTime.now());
            reminderHistoryRepository.save(new ReminderHistory(reminder, ReminderStatus.SENT, "sent by smtp"));
            return true;
        } catch (MailException exception) {
            String reason = exception.getMessage();
            if (reason == null || reason.isBlank()) {
                reason = exception.getClass().getName();
            }
            return failReminder(reminder, reason);
        }
    }

    private boolean isInTargetWindow(LocalDateTime now, LocalDateTime targetTime) {
        return !now.isBefore(targetTime) && now.isBefore(targetTime.plusMinutes(1));
    }

    private boolean hasTaskReminderAt(Task task, ReminderType type, LocalDateTime targetTime) {
        return reminderRepository.findByTaskTaskIdAndReminderTypeAndScheduledAtAfter(
                task.getTaskId(),
                type,
                targetTime.minusSeconds(1)
        ).stream().anyMatch(reminder -> targetTime.equals(reminder.getScheduledAt()));
    }

    private String emailSubject(Reminder reminder) {
        if (reminder.getReminderType() == ReminderType.DAILY_GOAL_DAY_END_ONE_HOUR
                || reminder.getReminderType() == ReminderType.DAILY_GOAL_DAY_END_THIRTY_MINUTES) {
            return "Daily goal reminder";
        }
        if (reminder.getReminderType() == ReminderType.DEADLINE_ONE_HOUR
                || reminder.getReminderType() == ReminderType.DEADLINE_THIRTY_MINUTES) {
            return "Deadline reminder";
        }
        return "Task reminder";
    }

    private boolean failReminder(Reminder reminder, String reason) {
        reminder.markFailed(reason);
        reminderHistoryRepository.save(new ReminderHistory(reminder, ReminderStatus.FAILED, reason));
        return false;
    }

    private void addIfNotNull(List<Reminder> reminders, Reminder reminder) {
        if (reminder != null) {
            reminders.add(reminder);
        }
    }
}
