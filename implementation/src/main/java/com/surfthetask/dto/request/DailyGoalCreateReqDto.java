package com.surfthetask.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DailyGoalCreateReqDto(
        @NotBlank @Size(max = 100) String title,
        String description,
        @NotNull @Positive Integer estimatedMinutes,
        @NotNull @Min(1) @Max(5) Integer importance,
        @NotNull @Positive Integer targetCountPerDay
) {
}
