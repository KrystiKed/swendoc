package at.technikum.swendoc.document;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

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

    /** Detected from the file on upload; null for anything that is not Word, PDF or Excel. */
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    protected Document() {
    }

    public Document(String title, String filename, String contentType, long size, String objectKey) {
        this.title = title;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.objectKey = objectKey;
        this.uploadedAt = Instant.now();
        this.documentType = DocumentType.detect(filename, contentType).orElse(null);
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

    public DocumentType getDocumentType() {
        return documentType;
    }
}
