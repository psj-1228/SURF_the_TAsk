package com.surfthetask.dto.request;

import jakarta.validation.constraints.NotNull;

public record FocusStartReqDto(
        @NotNull Long taskId
) {
}
