package com.surfthetask.dto.request;

import com.surfthetask.domain.enums.RepeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleReqDto(
        @NotBlank @Size(max = 100) String title,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        RepeatType repeatType
) {
}
