package com.surfthetask.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginReqDto(
        @NotBlank String loginId,
        @NotBlank String password
) {
}
