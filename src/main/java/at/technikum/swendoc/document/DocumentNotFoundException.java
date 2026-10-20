package at.technikum.swendoc.document;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID id) {
        super("No document with id " + id);
    }
}
