package com.surfthetask.dto.request;

import com.surfthetask.domain.enums.TaskStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record TaskUpdateReqDto(
        @NotBlank @Size(max = 100) String title,
        String description,
        @NotNull @Positive Integer estimatedMinutes,
        @NotNull @Min(1) @Max(5) Integer importance,
        TaskStatus status,
        @Positive Integer targetCountPerDay,
        @Future LocalDateTime deadlineAt,
        @Positive Integer warningThresholdHours
) {
}
