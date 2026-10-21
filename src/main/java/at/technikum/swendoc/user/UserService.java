package at.technikum.swendoc.user;

public interface UserService {

    /** Surrounding whitespace is dropped, so "alice " cannot be registered next to "alice". */
    User register(String username, String password);

    Session login(String username, String password);

    /** Ends the session server-side; the token is useless afterwards. */
    void logout(String authorization);

    /** Resolves "Bearer {token}" from POST /api/session to its user; 401 if missing, unknown or expired. */
    User authenticate(String authorization);

    /** The current password is asked again so a forgotten open session cannot lock the owner out. */
    User changePassword(User user, String currentPassword, String newPassword);
}
