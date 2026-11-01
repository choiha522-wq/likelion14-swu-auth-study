package com.example.authstudy.session;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 1주차 학습용 최소 세션 로그인. 실제 사용자 저장소 대신 고정 계정을 사용합니다. */
@RestController
public class SessionAuthController {
    private static final String LOGIN_USER = "LOGIN_USER";

    @PostMapping("/session/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request, HttpSession session) {
        if (!"study".equals(request.username()) || !"password".equals(request.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }
        session.setAttribute(LOGIN_USER, request.username());
        return ResponseEntity.ok(Map.of("message", "로그인되었습니다.", "sessionId", session.getId()));
    }

    @GetMapping("/session/me")
    public ResponseEntity<Map<String, String>> me(HttpSession session) {
        String username = (String) session.getAttribute(LOGIN_USER);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(Map.of("username", username));
    }

    @PostMapping("/session/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }

    public record LoginRequest(String username, String password) {}
}
