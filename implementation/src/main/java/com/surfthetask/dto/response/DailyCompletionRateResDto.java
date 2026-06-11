package com.surfthetask.dto.response;

import java.time.LocalDate;

public record DailyCompletionRateResDto(
        LocalDate date,
        String label,
        Integer completedCount,
        Integer totalTasks,
        Double completionRate
) {
}
