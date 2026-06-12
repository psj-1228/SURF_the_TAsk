package com.surfthetask;

import com.surfthetask.scheduler.DailyGoalRolloverScheduler;
import com.surfthetask.service.DailyGoalRolloverService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DailyGoalRolloverSchedulerTest {

    @Test
    void scheduledRolloverDelegatesToService() {
        DailyGoalRolloverService rolloverService = mock(DailyGoalRolloverService.class);
        DailyGoalRolloverScheduler scheduler = new DailyGoalRolloverScheduler(rolloverService);

        scheduler.runDailyGoalRollover();

        verify(rolloverService).rollOver();
    }
}
