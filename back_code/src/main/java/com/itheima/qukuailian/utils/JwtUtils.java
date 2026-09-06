package com.itheima.qukuailian.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;

/**
 * JWT 工具（jjwt 0.12.x）
 * <p>令牌头：Authorization: Bearer {token}；claims 中携带 userId/username/role/permissions。</p>
 */
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-seconds:43200}")
    private long expireSeconds;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成令牌
     */
    public String createToken(Long userId, String username, String role, Collection<String> permissions) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireSeconds * 1000))
                .signWith(key())
                .compact();
    }

    /**
     * 解析令牌；无效或过期会抛出 JwtException 等运行时异常
     */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
