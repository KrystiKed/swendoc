package at.technikum.swendoc.validation;

import jakarta.validation.ConstraintViolation;
import java.util.Set;

/** Validates one request type against the Bean Validation annotations on it. */
public interface Validator<T> {

    Set<ConstraintViolation<T>> validate(T obj);

    /** Returns obj unchanged, or throws {@link ValidationFailedException} (400 with field errors). */
    default T check(T obj) {
        Set<ConstraintViolation<T>> violations = validate(obj);
        if (!violations.isEmpty()) {
            throw new ValidationFailedException(violations);
        }
        return obj;
    }
}
