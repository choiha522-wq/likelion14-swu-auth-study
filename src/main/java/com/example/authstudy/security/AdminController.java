package com.example.authstudy.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminController {
    @GetMapping("/api/admin/ping")
    public Map<String, String> ping() {
        return Map.of("message", "관리자 권한 인증에 성공했습니다.");
    }
}
