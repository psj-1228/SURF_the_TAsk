package com.surfthetask.repository;

import com.surfthetask.domain.entity.DeadlineTask;
import com.surfthetask.domain.enums.TaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadlineTaskRepository extends JpaRepository<DeadlineTask, Long> {

    List<DeadlineTask> findByUserUserId(Long userId);

    List<DeadlineTask> findByUserUserIdAndStatusNot(Long userId, TaskStatus status);

    List<DeadlineTask> findByStatusNot(TaskStatus status);
}
