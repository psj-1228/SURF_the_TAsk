package com.surfthetask.repository;

import com.surfthetask.domain.entity.DailyGoal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyGoalRepository extends JpaRepository<DailyGoal, Long> {

    List<DailyGoal> findByUserUserId(Long userId);
}
