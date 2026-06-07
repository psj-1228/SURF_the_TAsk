package com.surfthetask.repository;

import com.surfthetask.domain.entity.CompletionRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletionRecordRepository extends JpaRepository<CompletionRecord, Long> {

    List<CompletionRecord> findByTaskTaskId(Long taskId);

    List<CompletionRecord> findByTaskUserUserId(Long userId);

    Optional<CompletionRecord> findTopByTaskTaskIdAndCanceledFalseOrderByCompletedAtDesc(Long taskId);

    void deleteByTaskTaskId(Long taskId);
}
