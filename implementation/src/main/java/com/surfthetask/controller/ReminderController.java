package com.surfthetask.controller;

import com.surfthetask.dto.request.NotificationPreferenceReqDto;
import com.surfthetask.dto.response.NotificationPreferenceResDto;
import com.surfthetask.dto.response.ReminderResDto;
import com.surfthetask.security.AuthenticatedUser;
import com.surfthetask.service.ReminderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping("/reminders")
    public List<ReminderResDto> getReminders(@AuthenticationPrincipal AuthenticatedUser user) {
        return reminderService.listReminders(user.userId()).stream().map(ReminderResDto::from).toList();
    }

    @PatchMapping("/notification-preference")
    public NotificationPreferenceResDto updatePreference(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody NotificationPreferenceReqDto req
    ) {
        return NotificationPreferenceResDto.from(reminderService.updatePreference(user.userId(), req));
    }
}
