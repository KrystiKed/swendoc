package at.technikum.swendoc.DocumentGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupNameRequest(
        @NotBlank(message = "{validation.required}") @Size(max = 255, message = "{validation.maxLength}") String name) {
}
