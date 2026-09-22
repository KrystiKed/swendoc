package at.technikum.swendoc.document;

import jakarta.validation.constraints.NotBlank;

public record TitleRequest(@NotBlank String title) {
}
