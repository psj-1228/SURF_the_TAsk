package com.surfthetask.repository;

import com.surfthetask.domain.entity.Reminder;
import com.surfthetask.domain.enums.AlertChannel;
import com.surfthetask.domain.enums.ReminderStatus;
import com.surfthetask.domain.enums.ReminderType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserUserIdOrderByScheduledAtDesc(Long userId);

    List<Reminder> findByTaskTaskId(Long taskId);

    List<Reminder> findByStatusAndScheduledAtLessThanEqual(ReminderStatus status, LocalDateTime now);

    List<Reminder> findByUserUserIdAndReminderTypeAndChannelAndScheduledAtAfter(
            Long userId,
            ReminderType reminderType,
            AlertChannel channel,
            LocalDateTime scheduledAfter
    );

    List<Reminder> findByTaskTaskIdAndReminderTypeAndScheduledAtAfter(
            Long taskId,
            ReminderType reminderType,
            LocalDateTime scheduledAfter
    );

    void deleteByTaskTaskId(Long taskId);
}
