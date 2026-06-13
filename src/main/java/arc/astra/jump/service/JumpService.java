package arc.astra.jump.service;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

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
        return cacheService.getUrl(code);
    }
}


