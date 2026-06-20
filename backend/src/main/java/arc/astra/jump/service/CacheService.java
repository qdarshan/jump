package arc.astra.jump.service;

import arc.astra.jump.model.JumpLink;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static arc.astra.jump.constant.Keys.*;

@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> rateLimitScript;

    public CacheService(RedisTemplate<String, Object> redisTemplate, RedisScript<Long> rateLimitScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
    }

    public long incrementCounter() {
        return redisTemplate.opsForValue().increment(URL_COUNTER_KEY);
    }

    public long incrementRateLimitCounter(@NonNull String key, long ttl) {
        Long count = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                ttl);

        return count != null ? count : 0L;
    }

    // store jump link and metadata
    public void cacheJumpLink(@NonNull JumpLink jumpLink) {
        storeJumpLink(jumpLink);
        storeMetadata(jumpLink);
    }

    private void storeJumpLink(@NonNull JumpLink jumpLink) {
        String key = String.format(URL_KEY_PATTERN, jumpLink.code());
        redisTemplate.opsForValue().set(key, jumpLink.url(), Duration.ofSeconds(remainingTtl(jumpLink.expiresAt())));
    }

    private void storeMetadata(@NonNull JumpLink jumpLink) {
        Map<String, String> values = Map.of(
                "url", jumpLink.url(),
                "createdBy", jumpLink.createdBy(),
                "createdAt", jumpLink.createdAt().toString()
        );
        String key = String.format(URL_META_KEY_PATTERN, jumpLink.code());
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, Duration.ofSeconds(remainingTtl(jumpLink.expiresAt())));
    }

    private long remainingTtl(Instant expiresAt) {
        return Math.max(0, Duration.between(Instant.now(), expiresAt).toSeconds());
    }

    public Map<String, String> getMetadata(@NonNull String code) {
        String key = String.format(URL_META_KEY_PATTERN, code);
        return redisTemplate.<String, String>opsForHash().entries(key);
    }

    public long getKeyTtl(@NonNull String code) {
        String key = String.format(URL_KEY_PATTERN, code);
        return redisTemplate.getExpire(key);
    }

    // record click and update the leaderboard
    public void trackClick(@NonNull String code, long ttlInSeconds) {
        String key = String.format(ANALYTICS_CLICKS_RECORD_PATTERN, code);
        redisTemplate.opsForList().leftPush(key, Instant.now().toString());
        redisTemplate.opsForList().trim(key, 0, 49);

        long safeTtl = Math.max(1, ttlInSeconds);
        redisTemplate.expire(key, Duration.ofSeconds(safeTtl));
        redisTemplate.opsForZSet().incrementScore(ANALYTICS_CLICKS_KEY, code, 1);
    }

    public List<Object> getRecordedClicks(@NonNull String code) {
        String key = String.format(ANALYTICS_CLICKS_RECORD_PATTERN, code);
        return redisTemplate.opsForList().range(key, 0, 49);
    }


    public long getClicks(@NonNull String code) {
        Double score = redisTemplate.opsForZSet().score(ANALYTICS_CLICKS_KEY, code);
        return score != null ? score.longValue() : 0L;
    }

    public Set<ZSetOperations.TypedTuple<Object>> getLeaderboard() {
        return redisTemplate.opsForZSet().reverseRangeWithScores(ANALYTICS_CLICKS_KEY, 0, 9);
    }
}
