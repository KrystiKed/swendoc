package at.technikum.swendoc.DocumentGroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import at.technikum.swendoc.document.DocumentResponse;

public record DocumentGroupResponse(UUID id, String name, Instant createdAt,
                                    List<DocumentResponse> documents) {
}
