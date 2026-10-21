package at.technikum.swendoc.document;

import java.util.List;
import java.util.UUID;

import at.technikum.swendoc.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByOwner(User owner);
}
