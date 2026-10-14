package at.technikum.swendoc.user;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Duration SESSION_TTL = Duration.ofHours(12);

    /** Unknown usernames are checked against this, so they take as long as a wrong password (no enumeration by timing). */
    private static final String DUMMY_HASH = Passwords.hash("no-such-user");

    private final UserRepository users;
    private final SessionRepository sessions;

    public UserService(UserRepository users, SessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    /** Surrounding whitespace is dropped, so "alice " cannot be registered next to "alice". */
    @Transactional
    public User register(String username, String password) {
        String name = username.strip();
        if (users.existsByUsername(name)) {
            throw new UsernameTakenException(name);
        }
        return users.save(new User(name, Passwords.hash(password)));
    }

    @Transactional
    public Session login(String username, String password) {
        Optional<User> user = users.findByUsername(username.strip());
        boolean matches = Passwords.matches(password, user.map(User::getPasswordHash).orElse(DUMMY_HASH));
        if (user.isEmpty() || !matches) {
            throw new InvalidCredentialsException();
        }
        return sessions.save(new Session(user.get(), Instant.now().plus(SESSION_TTL)));
    }

    /** Ends the session server-side; the token is useless afterwards. */
    @Transactional
    public void logout(String authorization) {
        sessions.deleteById(token(authorization));
    }

    /** Resolves "Bearer {token}" from POST /api/session to its user; 401 if missing, unknown or expired. */
    @Transactional(readOnly = true)
    public User authenticate(String authorization) {
        return sessions.findById(token(authorization))
                .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                .map(Session::getUser)
                .orElseThrow(InvalidCredentialsException::new);
    }

    /** The current password is asked again so a forgotten open session cannot lock the owner out. */
    @Transactional
    public User changePassword(User user, String currentPassword, String newPassword) {
        if (!Passwords.matches(currentPassword, user.getPasswordHash())) {
            throw new WrongPasswordException();
        }
        user.setPasswordHash(Passwords.hash(newPassword));
        return users.save(user);
    }

    private static UUID token(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new InvalidCredentialsException();
        }
        try {
            return UUID.fromString(authorization.substring("Bearer ".length()).trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException();
        }
    }
}
