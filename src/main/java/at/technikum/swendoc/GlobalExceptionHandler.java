package at.technikum.swendoc;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import at.technikum.swendoc.DocumentGroup.DocumentGroupNotFoundException;
import at.technikum.swendoc.document.DocumentNotFoundException;
import at.technikum.swendoc.storage.StorageException;
import at.technikum.swendoc.user.InvalidCredentialsException;
import at.technikum.swendoc.user.UsernameTakenException;
import at.technikum.swendoc.user.WrongPasswordException;
import at.technikum.swendoc.validation.ValidationFailedException;
import at.technikum.swendoc.validation.ValidationFailedException.FieldViolation;

/**
 * Maps every layer's exceptions to HTTP; the base class keeps Spring's own 4xx (bad UUID, missing part, ...).
 * Bodies are RFC 9457 problem details, except validation which lists its field errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Body: {"errors":[{"field":"title","constraint":"NotBlank","message":"..."}]}. */
    public record ValidationErrorResponse(List<FieldViolation> errors) {
    }

    // validation layer

    @ExceptionHandler(ValidationFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse validation(ValidationFailedException e) {
        log.debug("Validation failed: {}", e.getViolations());
        return new ValidationErrorResponse(e.getViolations());
    }

    // service layer

    @ExceptionHandler({DocumentNotFoundException.class, DocumentGroupNotFoundException.class})
    public ProblemDetail notFound(RuntimeException e) {
        log.debug(e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail unauthorized(InvalidCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ProblemDetail forbidden(WrongPasswordException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(UsernameTakenException.class)
    public ProblemDetail conflict(UsernameTakenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    // storage layer: the cause stays in the log, the client only learns that storage is down

    @ExceptionHandler(StorageException.class)
    public ProblemDetail storage(StorageException e) {
        log.error(e.getMessage(), e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "File storage is unavailable");
    }

    // anything unexpected: no stack trace or internals in the response

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }
}
