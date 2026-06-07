package com.surfthetask.scheduler;

import com.surfthetask.domain.entity.User;
import com.surfthetask.service.ReminderService;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {

    private final ReminderService reminderService;

    public ReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void runAvailabilityReminderCheck() {
        LocalDateTime now = LocalDateTime.now();
        for (User user : reminderService.findReminderUsers()) {
            reminderService.checkAvailabilityReminder(user, now);
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void runDeadlineReminderCheck() {
        LocalDateTime now = LocalDateTime.now();
        for (User user : reminderService.findReminderUsers()) {
            reminderService.checkDeadlineReminder(user, now);
        }
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void runDelayedInSiteAlertCheck() {
        reminderService.processDueReminders(LocalDateTime.now());
    }
}
