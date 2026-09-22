package at.technikum.swendoc.user;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {
}
