package com.surfthetask.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResDto(
        String code,
        String message,
        String path,
        LocalDateTime timestamp,
        List<String> details
) {

    public static ApiErrorResDto of(String code, String message, String path, List<String> details) {
        return new ApiErrorResDto(code, message, path, LocalDateTime.now(), details);
    }
}
