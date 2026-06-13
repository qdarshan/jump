package arc.astra.jump.service;

import arc.astra.jump.model.LeaderboardEntry;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class JumpService {

    private final CacheService cacheService;

    public JumpService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public String shortenUrl(@NonNull String url) {
        long count = cacheService.incrementCounter();
        String code = Base62Encoder.encode(count);
        cacheService.storeUrl(code, url);
        return code;
    }

    public boolean isRateLimited(@NonNull String ipAddress) {
        long count = cacheService.incrementRateLimitCounter(ipAddress, 60);
        return count > 10;
    }

    public String resolveUrl(@NonNull String code) {
        String url = cacheService.getUrl(code);
        if (url != null) {
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
                        tuple.getScore() != null ? tuple.getScore().longValue() : 0.0
                ))
                .toList();
    }
}


