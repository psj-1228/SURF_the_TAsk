package com.surfthetask.dto.response;

import com.surfthetask.domain.entity.User;
import java.time.LocalDateTime;

public record UserResDto(
        Long userId,
        String loginId,
        String name,
        String email,
        LocalDateTime createdAt
) {

    public static UserResDto from(User user) {
        return new UserResDto(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
