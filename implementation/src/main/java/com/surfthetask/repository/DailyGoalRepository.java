package com.surfthetask.repository;

import com.surfthetask.domain.entity.DailyGoal;
import com.surfthetask.domain.enums.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyGoalRepository extends JpaRepository<DailyGoal, Long> {

    List<DailyGoal> findByUserUserId(Long userId);

    List<DailyGoal> findByStatusInOrLastCompletedDateBefore(List<TaskStatus> statuses, LocalDate lastCompletedDate);
}
