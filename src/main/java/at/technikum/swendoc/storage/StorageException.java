package at.technikum.swendoc.storage;

/** The blob store could not be reached or rejected the request; mapped to 503. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
