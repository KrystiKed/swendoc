package at.technikum.swendoc.document;

import java.time.Instant;
import java.util.UUID;

/** API view of a document; deliberately omits objectKey, which is a storage detail. */
public record DocumentResponse(UUID id, String title, String filename, String contentType,
                               long size, Instant uploadedAt, DocumentType documentType) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(document.getId(), document.getTitle(), document.getFilename(),
                document.getContentType(), document.getSize(), document.getUploadedAt(),
                document.getDocumentType());
    }
}
