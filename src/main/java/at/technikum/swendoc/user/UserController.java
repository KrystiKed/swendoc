package at.technikum.swendoc.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> register(@RequestBody @Valid CredentialsRequest request) {
        User user = service.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/session")
    public SessionResponse login(@RequestBody @Valid CredentialsRequest request) {
        return SessionResponse.from(service.login(request.username(), request.password()));
    }
}
