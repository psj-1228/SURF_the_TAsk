package com.surfthetask.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthPageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/index";
    }

    @GetMapping("/schedule")
    public String schedule() {
        return "schedule/index";
    }

    @GetMapping("/daily-goals")
    public String dailyGoals() {
        return "tasks/daily-goals";
    }

    @GetMapping("/tasks")
    public String tasks() {
        return "tasks/deadline-tasks";
    }

    @GetMapping("/progress")
    public String progress() {
        return "progress/index";
    }

    @GetMapping("/reminders")
    public String reminders() {
        return "reminders/index";
    }

    @GetMapping("/focus")
    public String focus() {
        return "focus/index";
    }
}
