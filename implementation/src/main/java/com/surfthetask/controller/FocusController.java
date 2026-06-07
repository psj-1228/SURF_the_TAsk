package com.surfthetask.controller;

import com.surfthetask.dto.request.FocusFinishReqDto;
import com.surfthetask.dto.request.FocusStartReqDto;
import com.surfthetask.dto.response.FocusSessionResDto;
import com.surfthetask.security.AuthenticatedUser;
import com.surfthetask.service.FocusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FocusController {

    private final FocusService focusService;

    public FocusController(FocusService focusService) {
        this.focusService = focusService;
    }

    @PostMapping("/focus-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public FocusSessionResDto startFocus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FocusStartReqDto req
    ) {
        return FocusSessionResDto.from(focusService.startFocus(user.userId(), req.taskId()));
    }

    @PatchMapping("/focus-sessions/{sessionId}/finish")
    public FocusSessionResDto finishFocus(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) FocusFinishReqDto req
    ) {
        return FocusSessionResDto.from(focusService.finishFocus(user.userId(), sessionId, req));
    }
}
