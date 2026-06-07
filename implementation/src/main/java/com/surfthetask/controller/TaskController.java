package com.surfthetask.controller;

import com.surfthetask.dto.request.CompletionReqDto;
import com.surfthetask.dto.request.DailyGoalCreateReqDto;
import com.surfthetask.dto.request.DeadlineTaskCreateReqDto;
import com.surfthetask.dto.request.TaskUpdateReqDto;
import com.surfthetask.dto.response.CompletionRecordResDto;
import com.surfthetask.dto.response.TaskResDto;
import com.surfthetask.security.AuthenticatedUser;
import com.surfthetask.service.TaskService;
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
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/tasks/daily-goals")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResDto createDailyGoal(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody DailyGoalCreateReqDto req
    ) {
        return TaskResDto.from(taskService.createDailyGoal(user.userId(), req));
    }

    @PostMapping("/tasks/deadline-tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResDto createDeadlineTask(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody DeadlineTaskCreateReqDto req
    ) {
        return TaskResDto.from(taskService.createDeadlineTask(user.userId(), req));
    }

    @GetMapping("/tasks")
    public List<TaskResDto> getTasks(@AuthenticationPrincipal AuthenticatedUser user) {
        return taskService.getTasks(user.userId()).stream().map(TaskResDto::from).toList();
    }

    @GetMapping("/tasks/incomplete")
    public List<TaskResDto> getIncompleteTasks(@AuthenticationPrincipal AuthenticatedUser user) {
        return taskService.getIncompleteTasks(user.userId()).stream().map(TaskResDto::from).toList();
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResDto updateTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody TaskUpdateReqDto req
    ) {
        return TaskResDto.from(taskService.updateTask(user.userId(), taskId, req));
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        taskService.deleteTask(user.userId(), taskId);
    }

    @PostMapping("/tasks/{taskId}/completion")
    public CompletionRecordResDto completeTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) CompletionReqDto req
    ) {
        return CompletionRecordResDto.from(taskService.completeTask(user.userId(), taskId, req));
    }
}
