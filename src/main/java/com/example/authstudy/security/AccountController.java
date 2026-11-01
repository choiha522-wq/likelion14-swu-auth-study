package com.example.authstudy.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AccountController {
    @GetMapping({"/account/me", "/api/account/me"})
    public Map<String, Object> me(Authentication authentication) {
        return Map.of("username", authentication.getName(), "authorities", authentication.getAuthorities());
    }
}
