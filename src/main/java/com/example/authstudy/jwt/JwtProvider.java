package com.example.authstudy.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtProvider {
    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
                       @Value("${jwt.refresh-expiration-ms:1209600000}") long refreshExpirationMs) {
        // 학습 예제에서는 입력 문자열에서 키를 파생하지 않고 지정한 비밀 바이트를 사용합니다.
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret은 HS256을 위해 32바이트 이상이어야 합니다.");
        }
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secretBytes);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String createAccessToken(String username, List<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("roles", roles)
                .claim("tokenType", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String createRefreshToken(String username, List<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("roles", roles)
                .claim("tokenType", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
