package com.example.authstudy.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.Duration;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class TokenController {
    private final UserDetailsService users;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokens;
    private final boolean secureRefreshCookie;

    public TokenController(UserDetailsService users, PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
                           RefreshTokenStore refreshTokens,
                           @Value("${jwt.refresh-cookie-secure:true}") boolean secureRefreshCookie) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.refreshTokens = refreshTokens;
        this.secureRefreshCookie = secureRefreshCookie;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        try {
            UserDetails user = users.loadUserByUsername(request.username());
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                return loginFailed();
            }
            List<String> roles = user.getAuthorities().stream()
                    .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                    .toList();
            String accessToken = jwtProvider.createAccessToken(user.getUsername(), roles);
            String refreshToken = jwtProvider.createRefreshToken(user.getUsername(), roles);
            Claims refreshClaims = jwtProvider.parse(refreshToken);
            refreshTokens.save(user.getUsername(), refreshToken, refreshClaims.getExpiration().toInstant());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken).toString())
                    .body(Map.of("accessToken", accessToken));
        } catch (UsernameNotFoundException | NullPointerException e) {
            return loginFailed();
        }
    }

    @PostMapping("/reissue")
    public ResponseEntity<Map<String, String>> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @RequestHeader(value = "X-Refresh-Request", required = false) String refreshRequest) {
        if (!"true".equals(refreshRequest) || refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Refresh 쿠키와 X-Refresh-Request 헤더가 필요합니다."));
        }
        try {
            Claims oldClaims = jwtProvider.parse(refreshToken);
            if (!"refresh".equals(oldClaims.get("tokenType", String.class))) {
                return ResponseEntity.status(401).body(Map.of("message", "Refresh Token이 아닙니다."));
            }
            String username = oldClaims.getSubject();
            String newRefreshToken = jwtProvider.createRefreshToken(username, oldClaims.get("roles", List.class));
            Claims newClaims = jwtProvider.parse(newRefreshToken);
            if (!refreshTokens.rotate(username, refreshToken, newRefreshToken, newClaims.getExpiration().toInstant())) {
                return ResponseEntity.status(401).body(Map.of("message", "이미 사용됐거나 서버에서 폐기된 Refresh Token입니다."));
            }
            String accessToken = jwtProvider.createAccessToken(username, oldClaims.get("roles", List.class));
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(newRefreshToken).toString())
                    .body(Map.of("accessToken", accessToken));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("message", "만료되었거나 유효하지 않은 Refresh Token입니다."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @RequestHeader(value = "X-Refresh-Request", required = false) String refreshRequest) {
        if (!"true".equals(refreshRequest)) {
            return ResponseEntity.badRequest().body(Map.of("message", "X-Refresh-Request: true 헤더가 필요합니다."));
        }
        if (refreshToken != null) {
            try {
                Claims claims = jwtProvider.parse(refreshToken);
                if ("refresh".equals(claims.get("tokenType", String.class))) {
                    refreshTokens.deleteIfMatches(claims.getSubject(), refreshToken);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // 만료되거나 위조된 쿠키라도 클라이언트의 쿠키는 삭제합니다.
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(Map.of("message", "로그아웃되었습니다."));
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true).secure(secureRefreshCookie).sameSite("Strict")
                .path("/api/auth").maxAge(Duration.ofMillis(jwtProvider.getRefreshExpirationMs())).build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(secureRefreshCookie).sameSite("Strict")
                .path("/api/auth").maxAge(Duration.ZERO).build();
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
