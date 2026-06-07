package com.surfthetask.repository;

import com.surfthetask.domain.entity.FocusSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {

    Optional<FocusSession> findByUserUserIdAndActiveTrue(Long userId);

    List<FocusSession> findByActiveTrue();

    void deleteByTaskTaskId(Long taskId);
}
