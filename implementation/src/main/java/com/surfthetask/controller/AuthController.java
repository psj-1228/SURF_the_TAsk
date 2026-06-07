package com.surfthetask.controller;

import com.surfthetask.dto.request.LoginReqDto;
import com.surfthetask.dto.request.RegisterReqDto;
import com.surfthetask.dto.response.LoginResDto;
import com.surfthetask.dto.response.UserResDto;
import com.surfthetask.security.AuthenticatedUser;
import com.surfthetask.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResDto register(@Valid @RequestBody RegisterReqDto req) {
        return UserResDto.from(authService.register(req));
    }

    @PostMapping("/login")
    public LoginResDto login(@Valid @RequestBody LoginReqDto req) {
        return authService.login(req);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthenticatedUser user) {
        authService.logout(user.userId());
    }
}
