package at.technikum.swendoc.DocumentGroup;

import at.technikum.swendoc.document.Document;
import at.technikum.swendoc.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** A user-created collection of that user's own documents; a document may sit in several groups. */
@Entity
@Table(name = "document_groups")
public class DocumentGroup {

    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private Instant createdAt;

    @ManyToMany
    @JoinTable(name = "document_group_documents",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id"))
    private Set<Document> documents = new LinkedHashSet<>();

    protected DocumentGroup() {
    }

    public DocumentGroup(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Document> getDocuments() {
        return documents;
    }

    public void addDocument(Document document) {
        documents.add(document);
    }

    public void removeDocument(Document document) {
        documents.remove(document);
    }
}
