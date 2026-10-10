package at.technikum.swendoc.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TitleRequest(
        @NotBlank(message = "{validation.required}") @Size(max = 255, message = "{validation.maxLength}") String title) {
}
