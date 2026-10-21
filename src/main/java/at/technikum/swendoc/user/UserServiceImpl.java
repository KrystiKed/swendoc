package at.technikum.swendoc.user;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final Duration SESSION_TTL = Duration.ofHours(12);

    /** Unknown usernames are checked against this, so they take as long as a wrong password (no enumeration by timing). */
    private static final String DUMMY_HASH = Passwords.hash("no-such-user");

    private final UserRepository users;
    private final SessionRepository sessions;

    public UserServiceImpl(UserRepository users, SessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @Override
    @Transactional
    public User register(String username, String password) {
        String name = username.strip();
        if (users.existsByUsername(name)) {
            log.info("Registration rejected, username taken: {}", loggable(name));
            throw new UsernameTakenException(name);
        }
        User user = users.save(new User(name, Passwords.hash(password)));
        log.info("Registered user {} ({})", user.getId(), loggable(name));
        return user;
    }

    @Override
    @Transactional
    public Session login(String username, String password) {
        Optional<User> user = users.findByUsername(username.strip());
        boolean matches = Passwords.matches(password, user.map(User::getPasswordHash).orElse(DUMMY_HASH));
        if (user.isEmpty() || !matches) {
            log.warn("Failed login for username {}", loggable(username.strip()));
            throw new InvalidCredentialsException();
        }
        log.info("User {} logged in", user.get().getId());
        return sessions.save(new Session(user.get(), Instant.now().plus(SESSION_TTL)));
    }

    @Override
    @Transactional
    public void logout(String authorization) {
        sessions.deleteById(token(authorization));
        log.info("Session ended");
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(String authorization) {
        return sessions.findById(token(authorization))
                .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                .map(Session::getUser)
                .orElseThrow(InvalidCredentialsException::new);
    }

    @Override
    @Transactional
    public User changePassword(User user, String currentPassword, String newPassword) {
        if (!Passwords.matches(currentPassword, user.getPasswordHash())) {
            log.warn("Password change for user {} rejected: wrong current password", user.getId());
            throw new WrongPasswordException();
        }
        user.setPasswordHash(Passwords.hash(newPassword));
        log.info("User {} changed their password", user.getId());
        return users.save(user);
    }

    /** Usernames are client input; dropping line breaks keeps them from forging extra log lines. */
    private static String loggable(String username) {
        return username.replaceAll("[\\r\\n]", "_");
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
