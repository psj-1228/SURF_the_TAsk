package com.surfthetask.dto.request;

public record FocusFinishReqDto(
        Boolean actualFinished,
        Boolean completeTask
) {

    public boolean isActualFinished() {
        return Boolean.TRUE.equals(actualFinished);
    }

    public boolean shouldCompleteTask() {
        return Boolean.TRUE.equals(completeTask);
    }
}
