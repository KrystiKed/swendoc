package at.technikum.swendoc.validation;

import at.technikum.swendoc.validation.ValidationFailedException.FieldViolation;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ValidationExceptionHandler {

    /** Body: {"errors":[{"field":"title","constraint":"NotBlank","message":"..."}]}. */
    public record ValidationErrorResponse(List<FieldViolation> errors) {
    }

    @ExceptionHandler(ValidationFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handle(ValidationFailedException e) {
        return new ValidationErrorResponse(e.getViolations());
    }
}
