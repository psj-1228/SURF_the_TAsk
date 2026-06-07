package com.surfthetask.repository;

import com.surfthetask.domain.entity.Task;
import com.surfthetask.domain.enums.TaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    List<Task> findByUserUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, TaskStatus status);
}
