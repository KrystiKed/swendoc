package at.technikum.swendoc.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue
    private UUID token;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Instant createdAt;
    private Instant expiresAt;

    protected Session() {
    }

    public Session(User user, Instant expiresAt) {
        this.user = user;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public UUID getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
