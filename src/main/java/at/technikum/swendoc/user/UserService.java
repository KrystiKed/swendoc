package at.technikum.swendoc.user;

import java.time.Duration;
import java.time.Instant;
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
}
