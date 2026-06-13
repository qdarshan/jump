package arc.astra.jump.service;

import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class CacheService {

    // Keys
    private final static String URL_COUNTER_KEY = "jump:url:counter";
    private final static String RATE_LIMIT_KEY_PATTERN = "jump:ratelimit:%s";
    private final static String URL_KEY_PATTERN = "jump:url:%s";
    private final static String ANALYTICS_CLICKS_KEY = "jump:analytics:clicks";


    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> rateLimitScript;

    public CacheService(RedisTemplate<String, Object> redisTemplate, RedisScript<Long> rateLimitScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
    }

    public long incrementCounter() {
        return redisTemplate.opsForValue().increment(URL_COUNTER_KEY);
    }

    public long incrementRateLimitCounter(@NonNull String ipAddress, long ttl) {
        String key = String.format(RATE_LIMIT_KEY_PATTERN, ipAddress);

        Long count = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                ttl);

        return count != null ? count : 0L;
    }

    public void storeUrl(@NonNull String code, @NonNull String url) {
        String key = String.format(URL_KEY_PATTERN, code);
        redisTemplate.opsForValue().set(key, url);
    }

    public String getUrl(@NonNull String code) {
        String key = String.format(URL_KEY_PATTERN, code);
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void updateLeaderboard(String code) {
        redisTemplate.opsForZSet().incrementScore(ANALYTICS_CLICKS_KEY, code, 1);
    }

    public Set<ZSetOperations.TypedTuple<Object>> getLeaderboard() {
        return redisTemplate.opsForZSet().reverseRangeWithScores(ANALYTICS_CLICKS_KEY, 0, 9);
    }
}
