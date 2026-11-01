package com.example.authstudy.jwt;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** 데모용 단일 사용자-단일 세션 저장소. 프로세스 재시작 시 데이터가 사라집니다. */
@Component
public class RefreshTokenStore {
    private final ConcurrentHashMap<String, StoredToken> tokens = new ConcurrentHashMap<>();

    public void save(String username, String token, Instant expiresAt) {
        tokens.put(username, new StoredToken(hash(token), expiresAt));
    }

    public boolean rotate(String username, String oldToken, String newToken, Instant expiresAt) {
        String oldHash = hash(oldToken);
        String newHash = hash(newToken);
        final boolean[] changed = {false};
        tokens.computeIfPresent(username, (key, current) -> {
            if (current.expiresAt().isAfter(Instant.now())
                    && MessageDigest.isEqual(current.tokenHash().getBytes(StandardCharsets.UTF_8),
                    oldHash.getBytes(StandardCharsets.UTF_8))) {
                changed[0] = true;
                return new StoredToken(newHash, expiresAt);
            }
            return current;
        });
        return changed[0];
    }

    public boolean deleteIfMatches(String username, String presentedToken) {
        String presentedHash = hash(presentedToken);
        final boolean[] deleted = {false};
        tokens.computeIfPresent(username, (key, current) -> {
            if (current.expiresAt().isAfter(Instant.now())
                    && MessageDigest.isEqual(current.tokenHash().getBytes(StandardCharsets.UTF_8),
                    presentedHash.getBytes(StandardCharsets.UTF_8))) {
                deleted[0] = true;
                return null;
            }
            return current;
        });
        return deleted[0];
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    private record StoredToken(String tokenHash, Instant expiresAt) {}
}
