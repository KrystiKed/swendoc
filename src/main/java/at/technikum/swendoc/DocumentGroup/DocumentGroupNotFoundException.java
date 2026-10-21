package at.technikum.swendoc.DocumentGroup;

import java.util.UUID;

public class DocumentGroupNotFoundException extends RuntimeException {

    public DocumentGroupNotFoundException(UUID id) {
        super("No document group with id " + id);
    }
}
