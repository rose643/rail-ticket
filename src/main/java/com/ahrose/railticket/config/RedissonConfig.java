package com.ahrose.railticket.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** 为分布式锁创建独立的 Redisson 客户端，普通缓存仍使用 StringRedisTemplate。 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.database:0}") int database,
            @Value("${spring.data.redis.ssl.enabled:false}") boolean sslEnabled
    ) {
        String protocol = sslEnabled ? "rediss://" : "redis://";
        Config config = new Config();
        config.setLazyInitialization(true);
        var singleServerConfig = config.useSingleServer()
                .setAddress(protocol + host + ":" + port)
                .setDatabase(database);
        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }
        return Redisson.create(config);
    }
}
