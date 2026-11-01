package com.example.authstudy.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SignupController {
    private final UserDetailsManager users;
    private final PasswordEncoder passwordEncoder;

    public SignupController(UserDetailsManager users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody SignupRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("message", "아이디와 8자 이상 비밀번호를 입력해 주세요."));
        }
        if (users.userExists(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "이미 사용 중인 아이디입니다."));
        }
        users.createUser(User.withUsername(request.username()).password(passwordEncoder.encode(request.password())).roles("USER").build());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "가입되었습니다. /login에서 로그인해 주세요."));
    }

    public record SignupRequest(String username, String password) {}
}
