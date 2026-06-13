package arc.astra.jump.service;

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

@Service
public class CacheService {

    // Keys
    private final static String URL_COUNTER_KEY = "jump:url:counter";
    private final static String URL_KEY_PATTERN = "jump:url:%s";
    private final static String ANALYTICS_CLICKS_KEY = "jump:analytics:clicks";
    private final static String ANALYTICS_CLICKS_RECORD_PATTERN = "jump:analytics:clicks:%s";
    private final static String URL_META_KEY_PATTERN = "jump:url:meta:%s";


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

    public void storeUrl(@NonNull String code, @NonNull String url, long ttlInSeconds) {
        String key = String.format(URL_KEY_PATTERN, code);
        redisTemplate.opsForValue().set(key, url, Duration.ofSeconds(ttlInSeconds));
    }

    public void storeMetadata(@NonNull String code, @NonNull String url, @NonNull String email, long ttlInSeconds) {
        Map<String, String> values = Map.of(
                "url", url,
                "createdBy", email,
                "createdAt", Instant.now().toString()
        );
        String key = String.format(URL_META_KEY_PATTERN, code);
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, Duration.ofSeconds(ttlInSeconds));
    }

    public Map<String, String> getMetadata(@NonNull String code) {
        String key = String.format(URL_META_KEY_PATTERN, code);
        return redisTemplate.<String, String>opsForHash().entries(key);
    }

    public long getKeyTtl(@NonNull String code) {
        String key = String.format(URL_KEY_PATTERN, code);
        return redisTemplate.getExpire(key);
    }

    public String getUrl(@NonNull String code) {
        String key = String.format(URL_KEY_PATTERN, code);
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void updateLeaderboard(@NonNull String code) {
        redisTemplate.opsForZSet().incrementScore(ANALYTICS_CLICKS_KEY, code, 1);
    }

    public void recordClick(@NonNull String code, long ttlInSeconds) {
        String key = String.format(ANALYTICS_CLICKS_RECORD_PATTERN, code);
        redisTemplate.opsForList().leftPush(key, Instant.now().toString());
        redisTemplate.opsForList().trim(key, 0, 49);
        redisTemplate.expire(key, Duration.ofSeconds(ttlInSeconds));

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
