package at.technikum.swendoc.validation;

import org.springframework.stereotype.Component;

/**
 * Hands out a {@link Validator} per request type. A Spring bean instead of the static
 * Validation.buildDefaultValidatorFactory(): Spring's validator resolves messages from
 * messages*.properties in the request's Accept-Language, and a bean can be injected or mocked.
 */
@Component
public class ValidatorFactory {

    private final jakarta.validation.Validator delegate;

    public ValidatorFactory(jakarta.validation.Validator delegate) {
        this.delegate = delegate;
    }

    public <T> Validator<T> forClass(Class<T> clazz) {
        return obj -> delegate.validate(obj);
    }
}
