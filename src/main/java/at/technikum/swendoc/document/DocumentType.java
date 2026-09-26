package at.technikum.swendoc.document;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** The document types documents are grouped by, detected from the uploaded file. */
public enum DocumentType {

    WORD(Set.of("doc", "docx"), Set.of(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
    PDF(Set.of("pdf"), Set.of("application/pdf")),
    EXCEL(Set.of("xls", "xlsx"), Set.of(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    private final Set<String> extensions;
    private final Set<String> contentTypes;

    DocumentType(Set<String> extensions, Set<String> contentTypes) {
        this.extensions = extensions;
        this.contentTypes = contentTypes;
    }

    /**
     * Content type first, extension as fallback: browsers often send application/octet-stream
     * for Office files. Empty for anything that is not Word, PDF or Excel.
     */
    public static Optional<DocumentType> detect(String filename, String contentType) {
        String extension = filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        String mime = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        for (DocumentType type : values()) {
            if (type.contentTypes.contains(mime)) {
                return Optional.of(type);
            }
        }
        for (DocumentType type : values()) {
            if (type.extensions.contains(extension)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Case-insensitive, so /documents/group/pdf and /documents/group/PDF both work. */
    public static DocumentType fromPath(String value) {
        for (DocumentType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new UnknownDocumentTypeException(value);
    }
}
