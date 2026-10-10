package at.technikum.swendoc.document;

import at.technikum.swendoc.validation.FileSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/** Upload form; 50 MB matches spring.servlet.multipart.max-file-size. */
public record UploadRequest(
        @NotBlank(message = "{validation.required}") @Size(max = 255, message = "{validation.maxLength}") String title,
        @NotNull(message = "{validation.fileRequired}") @FileSize(maxMb = 50) MultipartFile file) {
}
