package com.surfthetask.dto.response;

import com.surfthetask.domain.entity.FocusSession;
import java.time.LocalDateTime;

public record FocusSessionResDto(
        Long sessionId,
        Long userId,
        Long taskId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean active,
        Boolean actualFinished,
        Long durationMinutes
) {

    public static FocusSessionResDto from(FocusSession session) {
        return new FocusSessionResDto(
                session.getSessionId(),
                session.getUser().getUserId(),
                session.getTask().getTaskId(),
                session.getStartAt(),
                session.getEndAt(),
                session.isActive(),
                session.getActualFinished(),
                session.getDurationMinutes()
        );
    }
}
