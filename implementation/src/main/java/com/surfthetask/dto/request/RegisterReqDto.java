package com.surfthetask.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterReqDto(
        @NotBlank @Size(max = 50) String loginId,
        @NotBlank @Size(min = 4, max = 100) String password,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Email @Size(max = 100) String email
) {
}
