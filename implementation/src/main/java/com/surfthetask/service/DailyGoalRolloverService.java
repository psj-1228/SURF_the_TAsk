package com.surfthetask.service;

import com.surfthetask.domain.entity.DailyGoal;
import com.surfthetask.domain.enums.TaskStatus;
import com.surfthetask.repository.DailyGoalRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DailyGoalRolloverService {

    private static final List<TaskStatus> ROLLOVER_STATUSES = List.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS);

    private final DailyGoalRepository dailyGoalRepository;

    public DailyGoalRolloverService(DailyGoalRepository dailyGoalRepository) {
        this.dailyGoalRepository = dailyGoalRepository;
    }

    @Transactional
    public int rollOver() {
        return rollOver(LocalDate.now());
    }

    @Transactional
    public int rollOver(LocalDate today) {
        Objects.requireNonNull(today, "today must not be null");

        int changedCount = 0;
        LocalDate yesterday = today.minusDays(1);
        List<DailyGoal> candidates = dailyGoalRepository.findByStatusInOrLastCompletedDateBefore(
                ROLLOVER_STATUSES,
                today
        );

        for (DailyGoal goal : candidates) {
            if (goal.isCompletedOn(today)) {
                continue;
            }
            if (rollOverGoal(goal, yesterday)) {
                changedCount++;
            }
        }
        return changedCount;
    }

    private boolean rollOverGoal(DailyGoal goal, LocalDate yesterday) {
        boolean changed = false;
        if (goal.getStatus() == TaskStatus.DONE || goal.getStatus() == TaskStatus.IN_PROGRESS) {
            goal.changeStatus(TaskStatus.TODO);
            changed = true;
        }

        LocalDate lastCompletedDate = goal.getLastCompletedDate();
        if (lastCompletedDate == null || lastCompletedDate.isBefore(yesterday)) {
            if (goal.getCurrentStreak() != 0) {
                goal.resetStreak();
                changed = true;
            }
        }
        return changed;
    }
}
