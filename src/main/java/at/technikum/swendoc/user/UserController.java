package at.technikum.swendoc.user;

import at.technikum.swendoc.validation.Validator;
import at.technikum.swendoc.validation.ValidatorFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService service;
    private final Validator<CredentialsRequest> credentials;

    public UserController(UserService service, ValidatorFactory validators) {
        this.service = service;
        this.credentials = validators.forClass(CredentialsRequest.class);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> register(@RequestBody CredentialsRequest request) {
        credentials.check(request);
        User user = service.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/session")
    public SessionResponse login(@RequestBody CredentialsRequest request) {
        credentials.check(request);
        return SessionResponse.from(service.login(request.username(), request.password()));
    }
}
