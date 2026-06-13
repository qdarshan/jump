package arc.astra.jump.model;

import java.time.Instant;
import java.util.List;

public record Metadata(
        String code,
        long totalClicks,
        List<Instant> recentClicks,
        String url,
        Instant createdAt,
        String createdBy,
        long expiresInSeconds
) {
}
