package com.example.authstudy.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class TokenController {
    private final UserDetailsService users;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public TokenController(UserDetailsService users, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        try {
            UserDetails user = users.loadUserByUsername(request.username());
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                return loginFailed();
            }
            return ResponseEntity.ok(Map.of("accessToken", jwtProvider.createAccessToken(user.getUsername())));
        } catch (UsernameNotFoundException | NullPointerException e) {
            return loginFailed();
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@RequestHeader(value = "Authorization", required = false) String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Authorization: Bearer <토큰> 형식이 필요합니다."));
        }
        try {
            Claims claims = jwtProvider.parse(header.substring(7));
            return ResponseEntity.ok(Map.of("subject", claims.getSubject(), "expiresAt", claims.getExpiration().toString()));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(401).body(Map.of("message", "토큰이 만료됐거나 유효하지 않습니다."));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("message", "토큰이 만료됐거나 유효하지 않습니다."));
        }
    }

    private ResponseEntity<Map<String, String>> loginFailed() {
        return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    public record LoginRequest(String username, String password) {}
}
