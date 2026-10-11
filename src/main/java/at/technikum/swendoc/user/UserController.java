package at.technikum.swendoc.user;

import at.technikum.swendoc.validation.Validator;
import at.technikum.swendoc.validation.ValidatorFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService service;
    private final Validator<CredentialsRequest> credentials;
    private final Validator<PasswordChangeRequest> passwordChanges;

    public UserController(UserService service, ValidatorFactory validators) {
        this.service = service;
        this.credentials = validators.forClass(CredentialsRequest.class);
        this.passwordChanges = validators.forClass(PasswordChangeRequest.class);
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

    /** The logged-in user, for the profile page. */
    @GetMapping("/users/me")
    public UserResponse me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return UserResponse.from(service.authenticate(authorization));
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody PasswordChangeRequest request) {
        User user = service.authenticate(authorization);
        passwordChanges.check(request);
        service.changePassword(user, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
