package com.surfthetask.dto.request;

public record CompletionReqDto(
        Boolean cancel
) {

    public boolean shouldCancel() {
        return Boolean.TRUE.equals(cancel);
    }
}
