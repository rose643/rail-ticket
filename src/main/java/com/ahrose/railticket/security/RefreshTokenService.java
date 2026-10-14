package com.ahrose.railticket.security;

import com.ahrose.railticket.bean.SysUser;
import com.ahrose.railticket.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/** 生成、轮换和撤销保存在 Redis 中的 Refresh Token。 */
@Service
public class RefreshTokenService {

    /** Refresh Token 存储前缀；完整格式：auth:refresh:{Token的SHA-256摘要}，值为用户ID。 */
    private static final String REDIS_KEY_PREFIX = "auth:refresh:";

    /** 生成Refresh Token使用的安全随机字节数；32字节对应256位随机强度。 */
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final long refreshExpireDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${security.jwt.refresh-expire-days}") long refreshExpireDays
    ) {
        if (refreshExpireDays <= 0) {
            throw new IllegalArgumentException("Refresh Token 有效期必须大于 0 天");
        }
        this.redisTemplate = redisTemplate;
        this.refreshExpireDays = refreshExpireDays;
    }

    /** 为用户签发随机 Refresh Token，Redis 中只保存 Token 摘要。 */
    public IssuedToken issue(SysUser user) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant expiresAt = Instant.now().plus(refreshExpireDays, ChronoUnit.DAYS);

        redisTemplate.opsForValue().set(
                redisKey(token),
                user.getId().toString(),
                Duration.ofDays(refreshExpireDays)
        );
        return new IssuedToken(token, expiresAt);
    }

    /** 原子读取并删除旧 Refresh Token，保证同一个 Token 只能刷新一次。 */
    public Long consume(String token) {
        String userId = redisTemplate.opsForValue().getAndDelete(redisKey(token));
        if (userId == null) {
            throw new BusinessException(401, "刷新令牌无效或已过期，请重新登录");
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(401, "刷新令牌无效，请重新登录");
        }
    }

    /** 撤销指定 Refresh Token；重复退出也按成功处理。 */
    public void revoke(String token) {
        redisTemplate.delete(redisKey(token));
    }

    private String redisKey(String token) {
        return REDIS_KEY_PREFIX + sha256(token.trim());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    /** 已签发的 Refresh Token 及其绝对过期时间。 */
    public record IssuedToken(String value, Instant expiresAt) {
    }
}
