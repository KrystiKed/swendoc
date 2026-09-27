package at.technikum.swendoc.document;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;

    @Mock
    private MinioClient minioClient;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(repository, minioClient, "test-bucket");
    }

    @Test
    void findReturnsDocumentWhenItExists() {
        UUID id = UUID.randomUUID();
        Document expectedDoc = new Document("Projektplan", "doc.pdf", "application/pdf", 1024, "key-123");
        when(repository.findById(id)).thenReturn(Optional.of(expectedDoc));

        Document result = service.find(id);

        assertNotNull(result);
        assertEquals("Projektplan", result.getTitle());
        verify(repository).findById(id);
    }

    @Test
    void findByTypeReturnsMatchingDocuments() {
        Document pdfDoc = new Document("Rechnung", "rechnung.pdf", "application/pdf", 2048, "key-456");
        when(repository.findByDocumentType(DocumentType.PDF)).thenReturn(List.of(pdfDoc));

        List<Document> result = service.findByType(DocumentType.PDF);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(DocumentType.PDF, result.getFirst().getDocumentType());
        verify(repository).findByDocumentType(DocumentType.PDF);
    }
}