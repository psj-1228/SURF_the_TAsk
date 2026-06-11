package com.surfthetask.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surfthetask.dto.response.LoginResDto;
import com.surfthetask.service.AuthService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("local")
public class DevLoginController {

    private static final String DEV_LOGIN_ID = "surf_dev";
    private static final String DEV_PASSWORD = "dev1234";
    private static final String DEV_NAME = "Surf Dev";
    private static final String DEV_EMAIL = "surf-dev@example.com";

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public DevLoginController(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/dev-login")
    public String devLogin(Model model) throws JsonProcessingException {
        LoginResDto login = authService.developmentLogin(DEV_LOGIN_ID, DEV_PASSWORD, DEV_NAME, DEV_EMAIL);
        model.addAttribute("surfUserJson", objectMapper.writeValueAsString(login));
        return "dev/login";
    }
}
