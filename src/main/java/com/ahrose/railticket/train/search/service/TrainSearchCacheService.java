package com.ahrose.railticket.train.search.service;

import com.ahrose.railticket.bean.response.TrainSearchResponse;
import com.ahrose.railticket.common.enums.TrainSearchLocationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** 查票结果 Redis 缓存，包含车次、起终点、票价和短时余票快照。 */
@Component
public class TrainSearchCacheService {

    /** 查票结果缓存前缀；完整格式：train:search:data:{日期}:{出发类型}:{出发值}:{到达类型}:{到达值}。 */
    private static final String CACHE_PREFIX = "train:search:data:";

    /** 运行日期反向索引前缀；完整格式：train:search:index:date:{travelDate}。 */
    private static final String DATE_INDEX_PREFIX = "train:search:index:date:";

    /** 运行计划反向索引前缀；完整格式：train:search:index:schedule:{scheduleId}。 */
    private static final String SCHEDULE_INDEX_PREFIX = "train:search:index:schedule:";

    /** 区段索引后缀；完整格式：train:search:index:schedule:{scheduleId}:segment:{segmentIndex}。 */
    private static final String SEGMENT_INDEX_PART = ":segment:";

    /** 缓存回源锁前缀；防止同一个查询条件失效时大量请求同时访问 MySQL。 */
    private static final String LOCK_PREFIX = "train:search:lock:";

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final JavaType responseListType;

    /** 查票结果基础TTL秒数，写入时会额外增加随机抖动，避免缓存集中失效。 */
    private final long ttlSeconds;

    /** 单个查询条件的Redis回源锁TTL秒数，防止服务异常后锁永久残留。 */
    private final long lockTtlSeconds;

    /** 未取得回源锁时，每次等待其他请求填充缓存的毫秒数。 */
    private final long waitMillis;

    public TrainSearchCacheService(
            StringRedisTemplate redisTemplate,
            RedissonClient redissonClient,
            ObjectMapper objectMapper,
            @Value("${train-search.cache.ttl-seconds:10}") long ttlSeconds,
            @Value("${train-search.cache.lock-ttl-seconds:5}") long lockTtlSeconds,
            @Value("${train-search.cache.wait-millis:50}") long waitMillis
    ) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.responseListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, TrainSearchResponse.class);
        this.ttlSeconds = Math.max(1, ttlSeconds);
        this.lockTtlSeconds = Math.max(1, lockTtlSeconds);
        this.waitMillis = Math.max(1, waitMillis);
    }

    /**
     * 优先读取 Redis；缓存未命中时由一个请求回源，其余请求短暂等待后读取结果。
     * Redis 不可用时直接执行数据库查询，保证查票功能可用。
     */
    public List<TrainSearchResponse> getOrLoad(
            TrainSearchLocationType fromType,
            String fromValue,
            TrainSearchLocationType toType,
            String toValue,
            LocalDate travelDate,
            Supplier<List<TrainSearchResponse>> loader
    ) {
        String cacheKey = buildCacheKey(fromType, fromValue, toType, toValue, travelDate);
        Optional<List<TrainSearchResponse>> cached = get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        String lockKey = LOCK_PREFIX + cacheKey.substring(CACHE_PREFIX.length());
        RLock lock = redissonClient.getLock(lockKey);
        Boolean lockAcquired = tryLock(lock);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                cached = get(cacheKey);
                if (cached.isPresent()) {
                    return cached.get();
                }
                List<TrainSearchResponse> result = loader.get();
                put(cacheKey, travelDate, result);
                return result;
            } finally {
                unlock(lock);
            }
        }

        if (lockAcquired == null) {
            return loader.get();
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            if (!pause()) {
                break;
            }
            cached = get(cacheKey);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        return loader.get();
    }

    /**
     * 事务提交后清除与指定售票区间重叠的查票缓存。
     * 订单占用区间为 {@code [fromStopIndex, toStopIndex)}。
     */
    public void evictSegmentsAfterCommit(
            Long scheduleId,
            Integer fromStopIndex,
            Integer toStopIndex
    ) {
        if (scheduleId == null || fromStopIndex == null || toStopIndex == null) {
            return;
        }
        runAfterCommit(() -> evictSegments(scheduleId, fromStopIndex, toStopIndex));
    }

    /** 事务提交后清除包含指定运行计划的全部查票缓存。 */
    public void evictScheduleAfterCommit(Long scheduleId) {
        if (scheduleId == null) {
            return;
        }
        runAfterCommit(() -> evictSchedule(scheduleId));
    }

    /**
     * 事务提交后清除指定运行日期的全部查票缓存。
     * 主要用于新增运行计划或将停用计划恢复为启用状态，因为新计划不可能存在于旧缓存的运行计划索引中。
     */
    public void evictTravelDateAfterCommit(LocalDate travelDate) {
        if (travelDate == null) {
            return;
        }
        runAfterCommit(() -> evictTravelDate(travelDate));
    }

    private Optional<List<TrainSearchResponse>> get(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, responseListType));
        } catch (JsonProcessingException exception) {
            safeDelete(cacheKey);
            return Optional.empty();
        } catch (DataAccessException exception) {
            return Optional.empty();
        }
    }

    private void put(
            String cacheKey,
            LocalDate travelDate,
            List<TrainSearchResponse> result
    ) {
        try {
            long actualTtl = ttlSeconds + ThreadLocalRandom.current()
                    .nextLong(Math.max(1, ttlSeconds / 2 + 1));
            Duration indexTtl = Duration.ofSeconds(actualTtl + 60);
            redisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(result),
                    Duration.ofSeconds(actualTtl)
            );
            indexCacheKey(cacheKey, travelDate, result, indexTtl);
        } catch (JsonProcessingException | DataAccessException ignored) {
            // 缓存失败不影响数据库查询结果。
        }
    }

    /** 返回 null 表示 Redis 不可用，false 表示锁被其他查询持有。 */
    private Boolean tryLock(RLock lock) {
        try {
            return lock.tryLock(0, lockTtlSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (RedisException exception) {
            return null;
        }
    }

    private void unlock(RLock lock) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (IllegalMonitorStateException | RedisException ignored) {
            // Redisson锁具有租约，Redis故障恢复后会自动过期，不会永久残留。
        }
    }

    private void indexCacheKey(
            String cacheKey,
            LocalDate travelDate,
            List<TrainSearchResponse> result,
            Duration indexTtl
    ) {
        Set<String> indexKeys = new HashSet<>();
        // 日期索引与查询结果是否为空无关，确保新增或恢复运行计划后能够清除旧结果和空结果缓存。
        indexKeys.add(dateIndexKey(travelDate));
        for (TrainSearchResponse response : result) {
            Long scheduleId = response.scheduleId();
            if (scheduleId == null) {
                continue;
            }
            indexKeys.add(scheduleIndexKey(scheduleId));
            for (int segment = response.fromStopIndex();
                 segment < response.toStopIndex();
                 segment++) {
                indexKeys.add(segmentIndexKey(scheduleId, segment));
            }
        }
        for (String indexKey : indexKeys) {
            redisTemplate.opsForSet().add(indexKey, cacheKey);
            redisTemplate.expire(indexKey, indexTtl);
        }
    }

    private void evictSegments(
            Long scheduleId,
            Integer fromStopIndex,
            Integer toStopIndex
    ) {
        Set<String> indexKeys = new HashSet<>();
        for (int segment = fromStopIndex; segment < toStopIndex; segment++) {
            indexKeys.add(segmentIndexKey(scheduleId, segment));
        }
        evictByIndexes(indexKeys);
    }

    private void evictSchedule(Long scheduleId) {
        evictByIndexes(Set.of(scheduleIndexKey(scheduleId)));
    }

    private void evictTravelDate(LocalDate travelDate) {
        evictByIndexes(Set.of(dateIndexKey(travelDate)));
    }

    private void evictByIndexes(Set<String> indexKeys) {
        try {
            Set<String> cacheKeys = new HashSet<>();
            for (String indexKey : indexKeys) {
                Set<String> members = redisTemplate.opsForSet().members(indexKey);
                if (members != null) {
                    cacheKeys.addAll(members);
                }
            }
            if (!cacheKeys.isEmpty()) {
                redisTemplate.delete(cacheKeys);
            }
            if (!indexKeys.isEmpty()) {
                redisTemplate.delete(indexKeys);
            }
        } catch (DataAccessException ignored) {
            // Redis 不可用时等待短 TTL 自然过期，MySQL 下单校验仍保证正确性。
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    }
            );
            return;
        }
        action.run();
    }

    private String scheduleIndexKey(Long scheduleId) {
        return SCHEDULE_INDEX_PREFIX + scheduleId;
    }

    private String dateIndexKey(LocalDate travelDate) {
        return DATE_INDEX_PREFIX + travelDate;
    }

    private String segmentIndexKey(Long scheduleId, Integer segmentIndex) {
        return scheduleIndexKey(scheduleId) + SEGMENT_INDEX_PART + segmentIndex;
    }

    private void safeDelete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException ignored) {
            // Redis 故障时忽略损坏缓存，直接回源数据库。
        }
    }

    private String buildCacheKey(
            TrainSearchLocationType fromType,
            String fromValue,
            TrainSearchLocationType toType,
            String toValue,
            LocalDate travelDate
    ) {
        return CACHE_PREFIX + travelDate
                + ":" + fromType.name() + ":" + fromValue
                + ":" + toType.name() + ":" + toValue;
    }

    private boolean pause() {
        try {
            Thread.sleep(waitMillis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
