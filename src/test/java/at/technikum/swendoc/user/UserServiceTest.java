package at.technikum.swendoc.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, sessionRepository);
    }

    @Test
    void registerSavesUserAndHashesPassword() {
        String username = "alice";
        String rawPassword = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User createdUser = userService.register(username, rawPassword);

        assertNotNull(createdUser);
        assertEquals(username, createdUser.getUsername());
        assertTrue(Passwords.matches(rawPassword, createdUser.getPasswordHash()));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginCreatesSessionWhenCredentialsAreValid() {
        String username = "alice";
        String rawPassword = "password123";
        String hashedPassword = Passwords.hash(rawPassword);
        User user = new User(username, hashedPassword);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session session = userService.login(username, rawPassword);

        assertNotNull(session);
        assertEquals(user, session.getUser());
        verify(sessionRepository).save(any(Session.class));
    }
}