package com.surfthetask.service;

import com.surfthetask.domain.entity.CompletionRecord;
import com.surfthetask.domain.entity.DailyGoal;
import com.surfthetask.domain.entity.DeadlineTask;
import com.surfthetask.domain.entity.FocusSession;
import com.surfthetask.domain.entity.Task;
import com.surfthetask.domain.enums.TaskStatus;
import com.surfthetask.dto.response.DailyCompletionRateResDto;
import com.surfthetask.dto.response.ProgressResDto;
import com.surfthetask.dto.response.TaskResDto;
import com.surfthetask.exception.NotFoundException;
import com.surfthetask.repository.CompletionRecordRepository;
import com.surfthetask.repository.DailyGoalRepository;
import com.surfthetask.repository.FocusSessionRepository;
import com.surfthetask.repository.TaskRepository;
import com.surfthetask.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProgressService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final CompletionRecordRepository completionRecordRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final PriorityCalculator priorityCalculator;

    public ProgressService(
            UserRepository userRepository,
            TaskRepository taskRepository,
            DailyGoalRepository dailyGoalRepository,
            CompletionRecordRepository completionRecordRepository,
            FocusSessionRepository focusSessionRepository,
            PriorityCalculator priorityCalculator
    ) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.dailyGoalRepository = dailyGoalRepository;
        this.completionRecordRepository = completionRecordRepository;
        this.focusSessionRepository = focusSessionRepository;
        this.priorityCalculator = priorityCalculator;
    }

    public ProgressResDto getProgress(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("user not found: " + userId);
        }

        List<Task> tasks = taskRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        int totalTasks = tasks.size();
        int totalDailyGoals = (int) tasks.stream().filter(DailyGoal.class::isInstance).count();
        int totalDeadlineTasks = (int) tasks.stream().filter(DeadlineTask.class::isInstance).count();
        int doneTasks = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        int incompleteTasks = totalTasks - doneTasks;
        double completionRate = rate(doneTasks, totalTasks);
        int bestStreak = dailyGoalRepository.findByUserUserId(userId)
                .stream()
                .mapToInt(DailyGoal::getCurrentStreak)
                .max()
                .orElse(0);
        LocalDate today = LocalDate.now();
        LocalDate currentWeekStart = today.minusDays(6);
        LocalDate previousWeekStart = today.minusDays(13);
        LocalDate previousWeekEnd = today.minusDays(7);

        List<CompletionRecord> currentWeekCompletions = completionRecordRepository
                .findByTaskUserUserIdAndCanceledFalseAndCompletedDateBetween(userId, currentWeekStart, today);
        List<CompletionRecord> previousWeekCompletions = completionRecordRepository
                .findByTaskUserUserIdAndCanceledFalseAndCompletedDateBetween(userId, previousWeekStart, previousWeekEnd);
        List<CompletionRecord> todayCompletions = currentWeekCompletions.stream()
                .filter(record -> today.equals(record.getCompletedDate()))
                .toList();
        int todayCompletedDailyGoals = completedTaskCount(todayCompletions, DailyGoal.class, totalDailyGoals);
        int todayCompletedDeadlineTasks = completedTaskCount(todayCompletions, DeadlineTask.class, totalDeadlineTasks);
        int todayCompletedTasks = todayCompletedDailyGoals + todayCompletedDeadlineTasks;
        double currentWeekCompletionRate = rate(currentWeekCompletions.size(), totalTasks);
        double previousWeekCompletionRate = rate(previousWeekCompletions.size(), totalTasks);
        double todayCompletionRate = rate(todayCompletedTasks, totalTasks);
        double weeklyCompletionRateDelta = roundOneDecimal(currentWeekCompletionRate - previousWeekCompletionRate);
        int totalFocusMinutes = Math.toIntExact(focusSessionRepository.findByUserUserIdAndEndAtIsNotNull(userId)
                .stream()
                .mapToLong(FocusSession::getDurationMinutes)
                .sum());
        List<DailyCompletionRateResDto> dailyCompletionRates = dailyCompletionRates(
                currentWeekStart,
                totalTasks,
                currentWeekCompletions
        );

        List<TaskResDto> priorityTasks = priorityCalculator
                .sortTasks(tasks.stream().filter(task -> task.getStatus() != TaskStatus.DONE).toList(), LocalDateTime.now())
                .stream()
                .limit(5)
                .map(TaskResDto::from)
                .toList();

        return new ProgressResDto(
                userId,
                totalTasks,
                doneTasks,
                incompleteTasks,
                completionRate,
                bestStreak,
                totalFocusMinutes,
                doneTasks,
                currentWeekCompletionRate,
                previousWeekCompletionRate,
                weeklyCompletionRateDelta,
                dailyCompletionRates,
                priorityTasks,
                todayCompletedDailyGoals,
                todayCompletedDeadlineTasks,
                todayCompletedTasks,
                todayCompletionRate
        );
    }

    private List<DailyCompletionRateResDto> dailyCompletionRates(
            LocalDate startDate,
            int totalTasks,
            List<CompletionRecord> completions
    ) {
        Map<LocalDate, Long> countByDate = completions.stream()
                .filter(record -> record.getTask().isCompleted())
                .collect(Collectors.groupingBy(
                        CompletionRecord::getCompletedDate,
                        Collectors.mapping(
                                record -> record.getTask().getTaskId(),
                                Collectors.collectingAndThen(
                                        Collectors.toSet(),
                                        taskIds -> (long) Math.min(taskIds.size(), totalTasks)
                                )
                        )
                ));

        return IntStream.range(0, 7)
                .mapToObj(index -> {
                    LocalDate date = startDate.plusDays(index);
                    int completedCount = countByDate.getOrDefault(date, 0L).intValue();
                    return new DailyCompletionRateResDto(
                            date,
                            "%d/%d".formatted(date.getMonthValue(), date.getDayOfMonth()),
                            completedCount,
                            totalTasks,
                            rate(completedCount, totalTasks)
                    );
                })
                .toList();
    }

    private int completedTaskCount(
            List<CompletionRecord> completions,
            Class<? extends Task> taskType,
            int maxCount
    ) {
        int count = Math.toIntExact(completions.stream()
                .map(CompletionRecord::getTask)
                .filter(taskType::isInstance)
                .filter(Task::isCompleted)
                .map(Task::getTaskId)
                .distinct()
                .count());
        return Math.min(count, maxCount);
    }

    private double rate(int completedCount, int totalTasks) {
        if (totalTasks == 0) {
            return 0.0;
        }
        return roundOneDecimal(completedCount * 100.0 / totalTasks);
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
