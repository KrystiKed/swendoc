package at.technikum.swendoc.DocumentGroup;

import java.util.List;
import java.util.UUID;

import at.technikum.swendoc.user.User;

public interface DocumentGroupService {

    List<DocumentGroup> findAll(User owner);

    /** A group of another user is reported as missing rather than forbidden, so ids do not leak. */
    DocumentGroup find(User owner, UUID id);

    DocumentGroup create(User owner, String name);

    DocumentGroup rename(User owner, UUID id, String name);

    void delete(User owner, UUID id);

    /** Only documents the user uploaded can go into their groups. */
    DocumentGroup addDocument(User owner, UUID groupId, UUID documentId);

    DocumentGroup removeDocument(User owner, UUID groupId, UUID documentId);
}
