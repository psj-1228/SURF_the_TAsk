package com.surfthetask.dto.response;

import com.surfthetask.domain.entity.User;

public record LoginResDto(
        Long userId,
        String loginId,
        String name,
        String token
) {

    public static LoginResDto of(User user, String token) {
        return new LoginResDto(user.getUserId(), user.getLoginId(), user.getName(), token);
    }
}
