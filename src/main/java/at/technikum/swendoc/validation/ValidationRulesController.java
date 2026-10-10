package at.technikum.swendoc.validation;

import at.technikum.swendoc.DocumentGroup.GroupNameRequest;
import at.technikum.swendoc.document.TitleRequest;
import at.technikum.swendoc.document.UploadRequest;
import at.technikum.swendoc.user.CredentialsRequest;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ValidationException;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Publishes the Bean Validation rules of each request type, so the frontend checks input with the
 * very rules the server enforces instead of a hand-copied set. Messages follow Accept-Language.
 */
@RestController
@RequestMapping("/validation")
public class ValidationRulesController {

    /** Forms the frontend can ask for; the annotations on these records are the single source of rules. */
    private static final Map<String, Class<?>> FORMS = Map.of(
            "credentials", CredentialsRequest.class,
            "upload", UploadRequest.class,
            "title", TitleRequest.class,
            "group", GroupNameRequest.class);

    private static final Set<String> NON_RULE_ATTRIBUTES = Set.of("message", "groups", "payload");
    private static final Set<String> REQUIRED = Set.of("NotNull", "NotBlank", "NotEmpty");

    public record Rule(String constraint, Map<String, Object> attributes, String message) {
    }

    private final jakarta.validation.ValidatorFactory validation;

    public ValidationRulesController(jakarta.validation.ValidatorFactory validation) {
        this.validation = validation;
    }

    /** field name -> rules, "required" rules first so a client can report the most basic problem. */
    @GetMapping("/{form}")
    public Map<String, List<Rule>> rules(@PathVariable String form) {
        Class<?> type = FORMS.get(form);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown form " + form);
        }
        Map<String, List<Rule>> rules = new TreeMap<>();
        validation.getValidator().getConstraintsForClass(type).getConstrainedProperties().forEach(property ->
                rules.put(property.getPropertyName(), property.getConstraintDescriptors().stream()
                        .map(this::rule)
                        .distinct()
                        .sorted(Comparator.comparing((Rule r) -> !REQUIRED.contains(r.constraint()))
                                .thenComparing(Rule::constraint))
                        .toList()));
        return rules;
    }

    private Rule rule(ConstraintDescriptor<?> descriptor) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        descriptor.getAttributes().forEach((name, value) -> {
            if (!NON_RULE_ATTRIBUTES.contains(name)) {
                attributes.put(name, value);
            }
        });
        // same interpolator (and therefore same wording and locale) as real violations
        String message = validation.getMessageInterpolator()
                .interpolate((String) descriptor.getAttributes().get("message"), context(descriptor));
        return new Rule(descriptor.getAnnotation().annotationType().getSimpleName(), attributes, message);
    }

    private static MessageInterpolator.Context context(ConstraintDescriptor<?> descriptor) {
        return new MessageInterpolator.Context() {
            @Override
            public ConstraintDescriptor<?> getConstraintDescriptor() {
                return descriptor;
            }

            @Override
            public Object getValidatedValue() {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> type) {
                throw new ValidationException("Not supported: " + type);
            }
        };
    }
}
