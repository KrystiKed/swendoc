package at.technikum.swendoc.storage;

import java.io.InputStream;

/** Blob store for document contents; failures surface as {@link StorageException}. */
public interface FileStorage {

    void store(String key, InputStream content, long size, String contentType);

    InputStream load(String key);

    void delete(String key);
}
