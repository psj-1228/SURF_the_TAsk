package com.surfthetask.service;

import com.surfthetask.domain.entity.DailyGoal;
import com.surfthetask.domain.entity.Task;
import com.surfthetask.domain.enums.TaskStatus;
import com.surfthetask.dto.response.ProgressResDto;
import com.surfthetask.dto.response.TaskResDto;
import com.surfthetask.exception.NotFoundException;
import com.surfthetask.repository.DailyGoalRepository;
import com.surfthetask.repository.TaskRepository;
import com.surfthetask.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProgressService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final PriorityCalculator priorityCalculator;

    public ProgressService(
            UserRepository userRepository,
            TaskRepository taskRepository,
            DailyGoalRepository dailyGoalRepository,
            PriorityCalculator priorityCalculator
    ) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.dailyGoalRepository = dailyGoalRepository;
        this.priorityCalculator = priorityCalculator;
    }

    public ProgressResDto getProgress(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user not found: " + userId);
        }

        List<Task> tasks = taskRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        int totalTasks = tasks.size();
        int doneTasks = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        int incompleteTasks = totalTasks - doneTasks;
        double completionRate = totalTasks == 0 ? 0.0 : Math.round((doneTasks * 1000.0 / totalTasks)) / 10.0;
        int bestStreak = dailyGoalRepository.findByUserUserId(userId)
                .stream()
                .mapToInt(DailyGoal::getCurrentStreak)
                .max()
                .orElse(0);

        List<TaskResDto> priorityTasks = priorityCalculator
                .sortTasks(tasks.stream().filter(task -> task.getStatus() != TaskStatus.DONE).toList(), LocalDateTime.now())
                .stream()
                .limit(5)
                .map(TaskResDto::from)
                .toList();

        return new ProgressResDto(userId, totalTasks, doneTasks, incompleteTasks, completionRate, bestStreak, priorityTasks);
    }
}
