package at.technikum.swendoc.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class FileSizeValidator implements ConstraintValidator<FileSize, MultipartFile> {

    private long maxBytes;

    @Override
    public void initialize(FileSize annotation) {
        maxBytes = annotation.maxMb() * 1024 * 1024;
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        return file == null || (file.getSize() > 0 && file.getSize() <= maxBytes);
    }
}
