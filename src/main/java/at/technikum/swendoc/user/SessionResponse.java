package at.technikum.swendoc.user;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID token, UUID userId, Instant expiresAt) {
}
