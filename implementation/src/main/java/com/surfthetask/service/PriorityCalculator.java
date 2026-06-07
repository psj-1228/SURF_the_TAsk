package com.surfthetask.service;

import com.surfthetask.domain.entity.DeadlineTask;
import com.surfthetask.domain.entity.Task;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PriorityCalculator {

    public double calculateScore(Task task, LocalDateTime now) {
        double score = task.getImportance() * 20.0;
        score += Math.max(0, 120 - task.getEstimatedMinutes()) / 6.0;

        if (task instanceof DeadlineTask deadlineTask) {
            if (deadlineTask.isOverdue(now)) {
                score += 100;
            } else if (deadlineTask.isDeadlineNear(now)) {
                score += 60;
            } else {
                long remainingHours = deadlineTask.getRemainingHours(now);
                score += Math.max(0, 48 - remainingHours);
            }
        }

        return score;
    }

    public List<Task> sortTasks(List<Task> tasks, LocalDateTime now) {
        return tasks.stream()
                .sorted(Comparator.comparingDouble((Task task) -> calculateScore(task, now)).reversed())
                .toList();
    }
}
