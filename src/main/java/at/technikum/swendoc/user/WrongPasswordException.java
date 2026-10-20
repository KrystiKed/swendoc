package at.technikum.swendoc.user;

/** 403, not 401: the session is fine, only the re-entered current password is wrong. */
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("Current password is wrong");
    }
}
