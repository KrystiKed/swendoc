package at.technikum.swendoc.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** An uploaded file must not be empty and at most maxMb megabytes; null is left to @NotNull. */
@Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = FileSizeValidator.class)
public @interface FileSize {

    long maxMb();

    String message() default "{validation.fileSize}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
