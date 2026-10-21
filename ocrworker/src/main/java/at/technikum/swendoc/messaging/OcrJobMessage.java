package messaging;

import java.util.UUID;

public record OcrJobMessage(UUID documentId, String objectKey) {}