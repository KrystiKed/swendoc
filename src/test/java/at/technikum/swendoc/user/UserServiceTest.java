package at.technikum.swendoc.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

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

    @Test
    void usernamesAreStoredAndLookedUpWithoutSurroundingWhitespace() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(UsernameTakenException.class, () -> userService.register("alice ", "password123"));
        assertThrows(InvalidCredentialsException.class, () -> userService.login(" bob ", "password123"));
        verify(userRepository).findByUsername("bob");
    }

    @Test
    void loginForAnUnknownUserIsRejected() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login("ghost", "password123"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void logoutDeletesTheSession() {
        UUID token = UUID.randomUUID();

        userService.logout("Bearer " + token);

        verify(sessionRepository).deleteById(token);
    }

    @Test
    void changePasswordStoresTheNewHash() {
        User user = new User("alice", Passwords.hash("old-password"));
        when(userRepository.save(user)).thenReturn(user);

        userService.changePassword(user, "old-password", "new-password");

        assertTrue(Passwords.matches("new-password", user.getPasswordHash()));
        assertFalse(Passwords.matches("old-password", user.getPasswordHash()));
    }

    @Test
    void changePasswordWithAWrongCurrentPasswordKeepsTheOldOne() {
        User user = new User("alice", Passwords.hash("old-password"));

        assertThrows(WrongPasswordException.class, () -> userService.changePassword(user, "nope-nope", "new-password"));

        assertTrue(Passwords.matches("old-password", user.getPasswordHash()));
        verify(userRepository, never()).save(any());
    }
}
