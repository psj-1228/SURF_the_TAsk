package com.surfthetask.scheduler;

import com.surfthetask.service.DailyGoalRolloverService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyGoalRolloverScheduler {

    private final DailyGoalRolloverService dailyGoalRolloverService;

    public DailyGoalRolloverScheduler(DailyGoalRolloverService dailyGoalRolloverService) {
        this.dailyGoalRolloverService = dailyGoalRolloverService;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void runDailyGoalRollover() {
        dailyGoalRolloverService.rollOver();
    }
}
