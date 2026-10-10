package at.technikum.swendoc.document;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import at.technikum.swendoc.user.User;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Document {

    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank
    private String title;

    private String filename;
    private String contentType;
    private long size;
    private Instant uploadedAt;

    /** Key of the blob in the MinIO bucket. */
    private String objectKey;

    /** Uploader; null for documents uploaded without a session token. */
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    protected Document() {
    }

    public Document(String title, String filename, String contentType, long size, String objectKey) {
        this.title = title;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.objectKey = objectKey;
        this.uploadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
