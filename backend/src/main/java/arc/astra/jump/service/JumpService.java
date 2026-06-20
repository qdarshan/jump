package arc.astra.jump.service;

import arc.astra.jump.dao.JumpLinkRepository;
import arc.astra.jump.exception.RateLimitExceedException;
import arc.astra.jump.exception.ResourceNotFoundException;
import arc.astra.jump.model.Analytics;
import arc.astra.jump.model.JumpLink;
import arc.astra.jump.model.LeaderboardEntry;
import arc.astra.jump.model.LinkResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static arc.astra.jump.constant.Keys.RATE_LIMIT_CLICK_PATTERN;
import static arc.astra.jump.constant.Keys.RATE_LIMIT_SHORTEN_PATTERN;

@Service
public class JumpService {

    private final static int MAX_REQUESTS_PER_WINDOW = 10;
    private final static int RATE_LIMIT_WINDOW_SECONDS = 60;
    private final static int MAX_CLICKS_PER_WINDOW = 1;
    private final static int CLICK_DEBOUNCE_SECONDS = 5;
    private final static int AUTO_EXPIRE_DURATION_IN_SECONDS = 604800;


    private final CacheService cacheService;
    private final JumpLinkRepository jumpLinkRepository;
    private final MeterRegistry meterRegistry;

    public JumpService(CacheService cacheService, JumpLinkRepository jumpLinkRepository, MeterRegistry meterRegistry) {
        this.cacheService = cacheService;
        this.jumpLinkRepository = jumpLinkRepository;
        this.meterRegistry = meterRegistry;
    }

    public LinkResponse shortenUrl(@NonNull String url, @NonNull String email) {

        if (isShortenRateLimited(email)) {

            meterRegistry.counter("jump.ratelimit.exceeded", "action", "shorten").increment();
            throw new RateLimitExceedException("Too many requests. You have temporarily exceeded your link generation quota. Please wait a moment and try again.");
        }

        long count = cacheService.incrementCounter();
        String code = Base62Encoder.encode(count);

        JumpLink jumpLink = new JumpLink(code, url, email, Instant.now(), Instant.now().plusSeconds(AUTO_EXPIRE_DURATION_IN_SECONDS));

        jumpLinkRepository.save(jumpLink);
        cacheService.cacheJumpLink(jumpLink);

        meterRegistry.counter("jump.links.created").increment();
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(code)
                .toUri();

        return new LinkResponse(code, location);
    }

    private boolean isShortenRateLimited(@NonNull String email) {
        String key = String.format(RATE_LIMIT_SHORTEN_PATTERN, email);
        long count = cacheService.incrementRateLimitCounter(key, RATE_LIMIT_WINDOW_SECONDS);
        return count > MAX_REQUESTS_PER_WINDOW;
    }

    public String resolveUrl(@NonNull String code, @NonNull String clientIp) {
        Map<String, String> metadata = cacheService.getMetadata(code);
        JumpLink jumpLink;
        if (metadata == null || metadata.isEmpty()) {
            meterRegistry.counter("jump.cache.lookups", "result", "miss", "operation", "resolveUrl").increment();
            jumpLink = fetchLinkFromSource(code);
            if (Instant.now().isAfter(jumpLink.expiresAt())) {
                throw new ResourceNotFoundException("This short code either does not exist or has expired.");
            }
            cacheService.cacheJumpLink(jumpLink);
        } else {
            meterRegistry.counter("jump.cache.lookups", "result", "hit", "operation", "resolveUrl").increment();
            jumpLink = new JumpLink(
                    code,
                    metadata.get("url"),
                    metadata.get("createdBy"),
                    Instant.parse(metadata.get("createdAt")),
                    Instant.now().plusSeconds(cacheService.getKeyTtl(code))
            );
        }

        if (!isClickSpam(code, clientIp)) {
            long remainingTtl = Math.max(0, Duration.between(Instant.now(), jumpLink.expiresAt()).toSeconds());
            cacheService.trackClick(code, remainingTtl);
        }
        return jumpLink.url();
    }

    private boolean isClickSpam(@NonNull String code, @NonNull String ipAddress) {
        String key = String.format(RATE_LIMIT_CLICK_PATTERN, code, ipAddress);
        long count = cacheService.incrementRateLimitCounter(key, CLICK_DEBOUNCE_SECONDS);
        return count > MAX_CLICKS_PER_WINDOW;
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

    public Analytics getStats(@NonNull String code) {
        String url;
        Instant createdAt;
        String createdBy;
        long expiresInSeconds;

        Map<String, String> metadata = cacheService.getMetadata(code);
        if (metadata == null || metadata.isEmpty()) {
            JumpLink jumpLink = fetchLinkFromSource(code);
            if (Instant.now().isAfter(jumpLink.expiresAt())) {
                throw new ResourceNotFoundException("This short code either does not exist or has expired.");
            }
            cacheService.cacheJumpLink(jumpLink);
            url = jumpLink.url();
            createdAt = jumpLink.createdAt();
            createdBy = jumpLink.createdBy();
            expiresInSeconds = Math.max(0, Duration.between(Instant.now(), jumpLink.expiresAt()).toSeconds());
        } else {
            url = metadata.get("url");
            createdAt = metadata.get("createdAt") == null ? null : Instant.parse(metadata.get("createdAt"));
            createdBy = metadata.get("createdBy");
            expiresInSeconds = Math.max(0, cacheService.getKeyTtl(code));
        }

        long totalClicks = cacheService.getClicks(code);
        List<Instant> recentClicks = cacheService.getRecordedClicks(code)
                .stream()
                .filter(c -> c instanceof String)
                .map(c -> Instant.parse((String) c))
                .toList();

        return new Analytics(
                code,
                totalClicks,
                recentClicks,
                url,
                createdAt,
                createdBy,
                expiresInSeconds
        );
    }

    private JumpLink fetchLinkFromSource(@NonNull String code) {
        return jumpLinkRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Jump link could not be found. It may have been entered incorrectly or has expired."));
    }


}


