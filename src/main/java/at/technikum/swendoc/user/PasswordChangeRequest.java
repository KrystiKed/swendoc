package at.technikum.swendoc.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "{validation.required}") String currentPassword,
        @NotBlank(message = "{validation.required}") @Size(min = 8, max = 128, message = "{validation.length}") String newPassword) {
}
