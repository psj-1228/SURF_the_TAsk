package com.surfthetask.dto.response;

import com.surfthetask.domain.entity.CompletionRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompletionRecordResDto(
        Long recordId,
        Long taskId,
        LocalDateTime completedAt,
        LocalDate completedDate,
        Boolean canceled
) {

    public static CompletionRecordResDto from(CompletionRecord record) {
        return new CompletionRecordResDto(
                record.getRecordId(),
                record.getTask().getTaskId(),
                record.getCompletedAt(),
                record.getCompletedDate(),
                record.isCanceled()
        );
    }
}
