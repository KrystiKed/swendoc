package at.technikum.swendoc.user;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final UserRepository users;
    private final SessionRepository sessions;

    public UserService(UserRepository users, SessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @Transactional
    public User register(String username, String password) {
        if (users.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        return users.save(new User(username, Passwords.hash(password)));
    }

    // ponytail: the token is issued but not yet enforced on /docs; add a filter when a sprint
    // actually requires protected endpoints.
    @Transactional
    public Session login(String username, String password) {
        User user = users.findByUsername(username)
                .filter(candidate -> Passwords.matches(password, candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return sessions.save(new Session(user, Instant.now().plus(SESSION_TTL)));
    }

    /** Resolves "Bearer {token}" from POST /session to its user; 401 if missing, unknown or expired. */
    @Transactional(readOnly = true)
    public User authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new InvalidCredentialsException();
        }
        UUID token;
        try {
            token = UUID.fromString(authorization.substring("Bearer ".length()).trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException();
        }
        return sessions.findById(token)
                .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                .map(Session::getUser)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
