package at.technikum.swendoc.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 403, not 401: the session is fine, only the re-entered current password is wrong. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("Current password is wrong");
    }
}
