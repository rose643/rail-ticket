package com.ahrose.railticket.train.search.service;

import com.ahrose.railticket.bean.response.TrainSearchResponse;
import com.ahrose.railticket.common.enums.TrainSearchLocationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainSearchCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private RLock lock;

    private TrainSearchCacheService cacheService;

    /** 每个测试执行前创建只依赖Mock对象的查票缓存服务。 */
    @BeforeEach
    void setUp() {
        cacheService = new TrainSearchCacheService(
                redisTemplate,
                redissonClient,
                new ObjectMapper(),
                10,
                5,
                1
        );
    }

    /** 验证查票结果为空时仍然写入日期反向索引，确保新增运行计划能够清除空缓存。 */
    @Test
    void getOrLoad_whenResultEmpty_shouldCreateTravelDateIndex() throws InterruptedException {
        LocalDate travelDate = LocalDate.of(2026, 10, 12);
        String cacheKey = "train:search:data:2026-10-12:CITY:上海:CITY:北京";
        String dateIndexKey = "train:search:index:date:2026-10-12";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0, 5, TimeUnit.SECONDS)).thenReturn(true);

        List<TrainSearchResponse> result = cacheService.getOrLoad(
                TrainSearchLocationType.CITY,
                "上海",
                TrainSearchLocationType.CITY,
                "北京",
                travelDate,
                List::of
        );

        org.junit.jupiter.api.Assertions.assertEquals(List.of(), result);
        verify(valueOperations).set(eq(cacheKey), eq("[]"), any(Duration.class));
        verify(setOperations).add(dateIndexKey, cacheKey);
        verify(redisTemplate).expire(eq(dateIndexKey), any(Duration.class));
    }

    /** 验证按日期失效时，会通过日期反向索引删除对应查票缓存和索引本身。 */
    @Test
    void evictTravelDateAfterCommit_shouldDeleteIndexedSearchCache() {
        LocalDate travelDate = LocalDate.of(2026, 10, 12);
        String cacheKey = "train:search:data:2026-10-12:CITY:上海:CITY:北京";
        String dateIndexKey = "train:search:index:date:2026-10-12";
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(dateIndexKey)).thenReturn(Set.of(cacheKey));

        cacheService.evictTravelDateAfterCommit(travelDate);

        verify(redisTemplate).delete(Set.of(cacheKey));
        verify(redisTemplate).delete(Set.of(dateIndexKey));
    }
}
