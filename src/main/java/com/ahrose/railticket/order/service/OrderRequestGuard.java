package com.ahrose.railticket.order.service;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 使用 Redis 短期拦截同一用户、同一幂等键的并发创建请求。 */
@Component
public class OrderRequestGuard {

    /** 下单防重复标记前缀；完整格式：order:create:{用户ID}:{客户端请求幂等键}。 */
    private static final String KEY_PREFIX = "order:create:";

    /** 下单防重复标记的固定值；这里只关心 Key 是否存在，不使用 Value 保存业务数据。 */
    private static final String GUARD_VALUE = "1";

    /** 下单防重复标记有效期；到期自动释放，防止异常请求形成永久阻塞。 */
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    public OrderRequestGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试占用请求幂等键。Redis 不可用时降级放行，由 MySQL 唯一索引最终兜底。
     */
    public boolean tryAcquire(Long userId, String requestId) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    KEY_PREFIX + userId + ":" + requestId,
                    GUARD_VALUE,
                    LOCK_TTL
            );
            return Boolean.TRUE.equals(acquired);
        } catch (DataAccessException exception) {
            return true;
        }
    }
}
