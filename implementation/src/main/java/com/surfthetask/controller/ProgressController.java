package com.surfthetask.controller;

import com.surfthetask.dto.response.ProgressResDto;
import com.surfthetask.security.AuthenticatedUser;
import com.surfthetask.service.ProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/progress")
    public ProgressResDto getProgress(@AuthenticationPrincipal AuthenticatedUser user) {
        return progressService.getProgress(user.userId());
    }
}
