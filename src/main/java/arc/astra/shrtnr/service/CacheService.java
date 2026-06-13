package arc.astra.shrtnr.service;

import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public int incrementCounter() {
        return Math.toIntExact(redisTemplate.opsForValue().increment("global:url:counter"));
    }

    public void setUrl(@NonNull String code,@NonNull String url) {
        String key = String.format("url:%s", code);
        try {
            redisTemplate.opsForValue().set(key, url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set url " + url, e);
        }
    }

    public String getURL(@NonNull String code) {
        String key = String.format("url:%s", code);
        try {
            return (String) redisTemplate.opsForValue().get(key);
        }  catch (Exception e) {
            throw new RuntimeException("Failed to retrieve url " + code, e);
        }
    }
}
