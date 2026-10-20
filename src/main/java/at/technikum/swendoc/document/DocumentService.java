package at.technikum.swendoc.document;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import at.technikum.swendoc.user.User;

/** Every operation is scoped to the owner: other users' documents do not exist for them. */
public interface DocumentService {

    List<Document> findAll(User owner);

    /** A document of another user is reported as missing rather than forbidden, so ids do not leak. */
    Document find(User owner, UUID id);

    Document upload(User owner, String title, String filename, String contentType, long size, InputStream content);

    InputStream download(Document document);

    Document rename(User owner, UUID id, String title);

    void delete(User owner, UUID id);
}
