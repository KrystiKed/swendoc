package at.technikum.swendoc.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username already taken: " + username);
    }
}
