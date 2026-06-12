package com.surfthetask;

import com.surfthetask.domain.entity.DailyGoal;
import com.surfthetask.domain.entity.DeadlineTask;
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
import com.surfthetask.repository.DailyGoalRepository;
import com.surfthetask.repository.DeadlineTaskRepository;
import com.surfthetask.repository.NotificationPreferenceRepository;
import com.surfthetask.repository.PersonalScheduleRepository;
import com.surfthetask.repository.ReminderHistoryRepository;
import com.surfthetask.repository.ReminderRepository;
import com.surfthetask.repository.TaskRepository;
import com.surfthetask.repository.UserRepository;
import com.surfthetask.service.PriorityCalculator;
import com.surfthetask.service.ReminderService;
import com.surfthetask.service.ScheduleAnalyzer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DeadlineTaskRepository deadlineTaskRepository;

    @Mock
    private DailyGoalRepository dailyGoalRepository;

    @Mock
    private PersonalScheduleRepository personalScheduleRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private ReminderHistoryRepository reminderHistoryRepository;

    @Mock
    private ScheduleAnalyzer scheduleAnalyzer;

    @Mock
    private PriorityCalculator priorityCalculator;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private ReminderService reminderService;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User("reminder_user", "hash", "Reminder User", "reminder@example.com");
        reminderService = new ReminderService(
                userRepository,
                taskRepository,
                deadlineTaskRepository,
                dailyGoalRepository,
                personalScheduleRepository,
                notificationPreferenceRepository,
                reminderRepository,
                reminderHistoryRepository,
                scheduleAnalyzer,
                priorityCalculator,
                mailSenderProvider,
                true,
                "noreply@surfthetask.local"
        );

        lenient().when(notificationPreferenceRepository.findByUserUserId(nullable(Long.class)))
                .thenReturn(Optional.of(new NotificationPreference(user)));
        lenient().when(reminderRepository.save(any(Reminder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void availabilityReminderCreatesSentInSiteReminderDuringAvailableTime() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 11, 10, 0);
        DailyGoal dailyGoal = new DailyGoal(user, "Read paper", "", 30, 4, 1);
        AvailabilitySlot slot = new AvailabilitySlot(
                now.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0)
        );

        when(personalScheduleRepository.findByUserUserId(nullable(Long.class))).thenReturn(List.of());
        when(scheduleAnalyzer.calculateAvailability(anyList())).thenReturn(List.of(slot));
        when(scheduleAnalyzer.isAvailable(eq(now), eq(List.of(slot)))).thenReturn(true);
        when(reminderRepository.findByUserUserIdAndReminderTypeAndChannelAndScheduledAtAfter(
                nullable(Long.class),
                eq(ReminderType.AVAILABILITY_BASED),
                eq(AlertChannel.IN_SITE),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(dailyGoalRepository.findByUserUserId(nullable(Long.class))).thenReturn(List.of(dailyGoal));
        when(deadlineTaskRepository.findByUserUserIdAndStatusNot(nullable(Long.class), eq(TaskStatus.DONE)))
                .thenReturn(List.of());
        when(priorityCalculator.sortTasks(anyList(), eq(now))).thenReturn(List.of(dailyGoal));

        Reminder reminder = reminderService.checkAvailabilityReminder(user, now);

        assertThat(reminder).isNotNull();
        assertThat(reminder.getChannel()).isEqualTo(AlertChannel.IN_SITE);
        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.AVAILABILITY_BASED);
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
        verify(reminderHistoryRepository).save(any(ReminderHistory.class));
    }

    @Test
    void availabilityReminderSkipsDuplicateInsideThirtyMinutes() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 11, 10, 0);
        DailyGoal dailyGoal = new DailyGoal(user, "Read paper", "", 30, 4, 1);
        Reminder recentReminder = new Reminder(
                user,
                dailyGoal,
                null,
                ReminderType.AVAILABILITY_BASED,
                AlertChannel.IN_SITE,
                "Recent reminder",
                now.minusMinutes(10)
        );

        when(personalScheduleRepository.findByUserUserId(nullable(Long.class))).thenReturn(List.of());
        when(scheduleAnalyzer.calculateAvailability(anyList())).thenReturn(List.of(
                new AvailabilitySlot(now.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(12, 0))
        ));
        when(scheduleAnalyzer.isAvailable(eq(now), anyList())).thenReturn(true);
        when(reminderRepository.findByUserUserIdAndReminderTypeAndChannelAndScheduledAtAfter(
                nullable(Long.class),
                eq(ReminderType.AVAILABILITY_BASED),
                eq(AlertChannel.IN_SITE),
                any(LocalDateTime.class)
        )).thenReturn(List.of(recentReminder));

        Reminder reminder = reminderService.checkAvailabilityReminder(user, now);

        assertThat(reminder).isNull();
        verify(reminderRepository, never()).save(any(Reminder.class));
    }

    @Test
    void dailyGoalEmailIsSentOneHourBeforeDayEndWhenGoalIsUnfinishedToday() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 11, 22, 59);
        DailyGoal dailyGoal = new DailyGoal(user, "Stretch", "", 10, 3, 1);

        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(deadlineTaskRepository.findByUserUserIdAndStatusNot(nullable(Long.class), eq(TaskStatus.DONE)))
                .thenReturn(List.of());
        when(dailyGoalRepository.findByUserUserId(nullable(Long.class))).thenReturn(List.of(dailyGoal));
        when(reminderRepository.findByTaskTaskIdAndReminderTypeAndScheduledAtAfter(
                nullable(Long.class),
                eq(ReminderType.DAILY_GOAL_DAY_END_ONE_HOUR),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        List<Reminder> reminders = reminderService.checkDeadlineReminder(user, now);

        assertThat(reminders).hasSize(1);
        assertThat(reminders.get(0).getReminderType()).isEqualTo(ReminderType.DAILY_GOAL_DAY_END_ONE_HOUR);
        assertThat(reminders.get(0).getStatus()).isEqualTo(ReminderStatus.SENT);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void dailyGoalEmailIsNotSentWhenGoalWasCompletedToday() {
        LocalDate today = LocalDate.of(2026, 6, 11);
        LocalDateTime now = today.atTime(22, 59);
        DailyGoal dailyGoal = new DailyGoal(user, "Stretch", "", 10, 3, 1);
        dailyGoal.recordCompletion(today);

        when(deadlineTaskRepository.findByUserUserIdAndStatusNot(nullable(Long.class), eq(TaskStatus.DONE)))
                .thenReturn(List.of());
        when(dailyGoalRepository.findByUserUserId(nullable(Long.class))).thenReturn(List.of(dailyGoal));

        List<Reminder> reminders = reminderService.checkDeadlineReminder(user, now);

        assertThat(reminders).isEmpty();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void deadlineTaskEmailIsSentOneHourBeforeDeadline() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 11, 13, 0);
        DeadlineTask task = new DeadlineTask(
                user,
                "Submit report",
                "",
                60,
                5,
                now.plusHours(1),
                24
        );

        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(dailyGoalRepository.findByUserUserId(nullable(Long.class))).thenReturn(List.of());
        when(deadlineTaskRepository.findByUserUserIdAndStatusNot(nullable(Long.class), eq(TaskStatus.DONE)))
                .thenReturn(List.of(task));
        when(reminderRepository.findByTaskTaskIdAndReminderTypeAndScheduledAtAfter(
                nullable(Long.class),
                eq(ReminderType.DEADLINE_ONE_HOUR),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        List<Reminder> reminders = reminderService.checkDeadlineReminder(user, now);

        assertThat(reminders).hasSize(1);
        assertThat(reminders.get(0).getReminderType()).isEqualTo(ReminderType.DEADLINE_ONE_HOUR);
        assertThat(reminders.get(0).getScheduledAt()).isEqualTo(now);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void deadlineTaskEmailStillSendsWhenSchedulerRunsShortlyAfterTargetTime() {
        LocalDateTime targetTime = LocalDateTime.of(2026, 6, 11, 13, 0);
        LocalDateTime now = targetTime.plusMinutes(2);
        DeadlineTask task = new DeadlineTask(
                user,
                "Submit report",
                "",
                60,
                5,
                targetTime.plusHours(1),
                24
        );

        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(dailyGoalRepository.findByUserUserId(nullable(Long.class))).thenReturn(List.of());
        when(deadlineTaskRepository.findByUserUserIdAndStatusNot(nullable(Long.class), eq(TaskStatus.DONE)))
                .thenReturn(List.of(task));
        when(reminderRepository.findByTaskTaskIdAndReminderTypeAndScheduledAtAfter(
                nullable(Long.class),
                eq(ReminderType.DEADLINE_ONE_HOUR),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        List<Reminder> reminders = reminderService.checkDeadlineReminder(user, now);

        assertThat(reminders).hasSize(1);
        assertThat(reminders.get(0).getReminderType()).isEqualTo(ReminderType.DEADLINE_ONE_HOUR);
        assertThat(reminders.get(0).getScheduledAt()).isEqualTo(targetTime);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void deadlineTaskEmailIgnoresLaterSameTypeReminderWhenCurrentTargetHasNotBeenSent() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 11, 13, 0);
        DeadlineTask task = new DeadlineTask(
                user,
                "Submit report",
                "",
                60,
                5,
                now.plusHours(1),
                24
        );
        Reminder laterReminder = new Reminder(
                user,
                task,
                null,
                ReminderType.DEADLINE_ONE_HOUR,
                AlertChannel.EMAIL,
                "Deadline is due in 1 hour: Submit report",
                now.plusDays(1)
        );

        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(dailyGoalRepository.findByUserUserId(nullable(Long.class))).thenReturn(List.of());
        when(deadlineTaskRepository.findByUserUserIdAndStatusNot(nullable(Long.class), eq(TaskStatus.DONE)))
                .thenReturn(List.of(task));
        when(reminderRepository.findByTaskTaskIdAndReminderTypeAndScheduledAtAfter(
                nullable(Long.class),
                eq(ReminderType.DEADLINE_ONE_HOUR),
                any(LocalDateTime.class)
        )).thenReturn(List.of(laterReminder));

        List<Reminder> reminders = reminderService.checkDeadlineReminder(user, now);

        assertThat(reminders).hasSize(1);
        assertThat(reminders.get(0).getReminderType()).isEqualTo(ReminderType.DEADLINE_ONE_HOUR);
        assertThat(reminders.get(0).getScheduledAt()).isEqualTo(now);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void emailFailureMarksReminderFailedAndRecordsHistory() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 11, 22, 59);
        DailyGoal dailyGoal = new DailyGoal(user, "Stretch", "", 10, 3, 1);
        Reminder reminder = new Reminder(
                user,
                dailyGoal,
                null,
                ReminderType.DAILY_GOAL_DAY_END_ONE_HOUR,
                AlertChannel.EMAIL,
                "Daily Goal is due soon: Stretch",
                now
        );

        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        boolean sent = reminderService.sendReminder(reminder);

        assertThat(sent).isFalse();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.FAILED);
        ArgumentCaptor<ReminderHistory> historyCaptor = ArgumentCaptor.forClass(ReminderHistory.class);
        verify(reminderHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getResultStatus()).isEqualTo(ReminderStatus.FAILED);
    }
}
