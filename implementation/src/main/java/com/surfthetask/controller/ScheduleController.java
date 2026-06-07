package com.surfthetask.controller;

import com.surfthetask.dto.request.ScheduleReqDto;
import com.surfthetask.dto.response.AvailabilitySlotResDto;
import com.surfthetask.dto.response.ScheduleResDto;
import com.surfthetask.security.AuthenticatedUser;
import com.surfthetask.service.ScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResDto createSchedule(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ScheduleReqDto req
    ) {
        return ScheduleResDto.from(scheduleService.createSchedule(user.userId(), req));
    }

    @GetMapping("/schedules")
    public List<ScheduleResDto> getSchedules(@AuthenticationPrincipal AuthenticatedUser user) {
        return scheduleService.getSchedules(user.userId()).stream().map(ScheduleResDto::from).toList();
    }

    @PutMapping("/schedules/{scheduleId}")
    public ScheduleResDto updateSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ScheduleReqDto req
    ) {
        return ScheduleResDto.from(scheduleService.updateSchedule(user.userId(), scheduleId, req));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchedule(@PathVariable Long scheduleId, @AuthenticationPrincipal AuthenticatedUser user) {
        scheduleService.deleteSchedule(user.userId(), scheduleId);
    }

    @GetMapping("/availability")
    public List<AvailabilitySlotResDto> getAvailability(@AuthenticationPrincipal AuthenticatedUser user) {
        return scheduleService.calculateAvailability(user.userId()).stream().map(AvailabilitySlotResDto::from).toList();
    }
}
