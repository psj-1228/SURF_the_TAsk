package com.surfthetask.security;

public record AuthenticatedUser(
        Long userId,
        String loginId,
        String name
) {
}
