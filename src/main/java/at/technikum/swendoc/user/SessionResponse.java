package at.technikum.swendoc.user;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID token, UUID userId, Instant expiresAt) {

    public static SessionResponse from(Session session) {
        return new SessionResponse(session.getToken(), session.getUser().getId(), session.getExpiresAt());
    }
}
