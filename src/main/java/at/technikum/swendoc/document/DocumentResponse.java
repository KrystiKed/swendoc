package at.technikum.swendoc.document;

import java.time.Instant;
import java.util.UUID;

/** API view of a document; deliberately omits objectKey, which is a storage detail. */
public record DocumentResponse(UUID id, String title, String filename, String contentType,
                               long size, Instant uploadedAt) {
}
