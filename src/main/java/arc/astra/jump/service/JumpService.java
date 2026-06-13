package arc.astra.jump.service;

import arc.astra.jump.model.LeaderboardEntry;
import arc.astra.jump.model.Metadata;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class JumpService {

    private final static int RATE_LIMIT_TTL = 60;
    private final static int AUTO_EXPIRE_DURATION_IN_SECONDS = 604800;


    private final CacheService cacheService;

    public JumpService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public String shortenUrl(@NonNull String url, @NonNull String ipAddress) {
        long count = cacheService.incrementCounter();
        String code = Base62Encoder.encode(count);
        cacheService.storeUrl(code, url, AUTO_EXPIRE_DURATION_IN_SECONDS);
        cacheService.storeMetadata(code, url, ipAddress, AUTO_EXPIRE_DURATION_IN_SECONDS);

        return code;
    }

    public boolean isRateLimited(@NonNull String ipAddress) {
        long count = cacheService.incrementRateLimitCounter(ipAddress, RATE_LIMIT_TTL);
        return count > 10;
    }

    public String resolveUrl(@NonNull String code) {
        String url = cacheService.getUrl(code);
        if (url != null) {
            cacheService.recordClick(code, AUTO_EXPIRE_DURATION_IN_SECONDS);
            cacheService.updateLeaderboard(code);
        }
        return url;

    }

    public List<LeaderboardEntry> getLeaderboard() {
        Set<ZSetOperations.TypedTuple<Object>> rawEntries = cacheService.getLeaderboard();

        if (rawEntries == null || rawEntries.isEmpty()) {
            return List.of();
        }

        return rawEntries.stream()
                .map(tuple -> new LeaderboardEntry(
                        String.valueOf(tuple.getValue()),
                        tuple.getScore() != null ? tuple.getScore().longValue() : 0L
                ))
                .toList();
    }

    public Metadata getMetadata(@NonNull String code) {
        String url = cacheService.getUrl(code);

        if (url == null) {
            throw new IllegalArgumentException("code not found or expired");
        }

        long totalClicks = cacheService.getClicks(code);
        Map<String, String> metadata = cacheService.getMetadata(code);
        Instant createdAt = (metadata.get("createdAt") == null ? null : Instant.parse(String.valueOf(metadata.get("createdAt"))));
        long expiresInSeconds = Math.max(0, cacheService.getKeyTtl(code));

        List<Instant> recentClicks = cacheService.getRecordedClicks(code)
                .stream()
                .map(c -> Instant.parse((String) c))
                .toList();

        return new Metadata(
                code,
                totalClicks,
                recentClicks,
                url,
                createdAt,
                metadata.get("createdBy"),
                expiresInSeconds
        );
    }
}


