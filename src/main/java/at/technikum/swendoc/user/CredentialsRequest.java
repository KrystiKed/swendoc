package at.technikum.swendoc.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CredentialsRequest(
        @NotBlank(message = "{validation.required}") @Size(max = 64, message = "{validation.maxLength}") String username,
        @NotBlank(message = "{validation.required}") @Size(min = 8, max = 128, message = "{validation.length}") String password) {
}
