package at.technikum.swendoc.DocumentGroup;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import at.technikum.swendoc.document.Document;
import at.technikum.swendoc.user.User;

public interface DocumentGroupRepository extends JpaRepository<DocumentGroup, UUID> {

    List<DocumentGroup> findByOwner(User owner);

    List<DocumentGroup> findByDocumentsContaining(Document document);
}
