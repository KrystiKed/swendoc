package at.technikum.swendoc.user;

import java.time.Instant;
import java.util.UUID;

/** API view of a user; exists so passwordHash can never reach a response body. */
public record UserResponse(UUID id, String username, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }
}
