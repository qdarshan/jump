package arc.astra.jump.model;

import java.time.Instant;

public record JumpLink(
        String code,
        String url,
        String createdBy,
        Instant createdAt,
        Instant expiresAt
) {
}
