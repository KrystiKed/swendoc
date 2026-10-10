package at.technikum.swendoc.validation;

import jakarta.validation.ConstraintViolation;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Turned into a 400 with one entry per violated rule by {@link ValidationExceptionHandler}. */
public class ValidationFailedException extends RuntimeException {

    private final List<FieldViolation> violations;

    public ValidationFailedException(Set<? extends ConstraintViolation<?>> violations) {
        super("Validation failed");
        this.violations = violations.stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(),
                        v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        v.getMessage()))
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::constraint))
                .toList();
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }

    /** field is the request property ("title"), message is already localized. */
    public record FieldViolation(String field, String constraint, String message) {
    }
}
