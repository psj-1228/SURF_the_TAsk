package com.surfthetask.repository;

import com.surfthetask.domain.entity.ReminderHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderHistoryRepository extends JpaRepository<ReminderHistory, Long> {

    List<ReminderHistory> findByReminderReminderId(Long reminderId);

    void deleteByReminderReminderId(Long reminderId);
}
