package at.technikum.swendoc.document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import at.technikum.swendoc.DocumentGroup.DocumentGroup;
import at.technikum.swendoc.DocumentGroup.DocumentGroupRepository;
import at.technikum.swendoc.messaging.OcrJobPublisher;
import at.technikum.swendoc.storage.FileStorage;
import at.technikum.swendoc.storage.StorageException;
import at.technikum.swendoc.user.User;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documents;

    @Mock
    private DocumentGroupRepository groups;

    @Mock
    private FileStorage storage;

    @Mock
    private OcrJobPublisher ocrJobs;

    private DocumentService service;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        service = new DocumentServiceImpl(documents, groups, storage, ocrJobs);
        alice = user("alice");
        bob = user("bob");
    }

    @Test
    void uploadStoresTheBlobAndSavesTheDocumentForTheOwner() {
        InputStream content = new ByteArrayInputStream("pdf".getBytes());
        when(documents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Document saved = service.upload(alice, "Invoice", "invoice.pdf", "application/pdf", 3, content);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).store(key.capture(), eq(content), eq(3L), eq("application/pdf"));
        assertEquals(key.getValue(), saved.getObjectKey());
        assertEquals(alice, saved.getOwner());
        assertEquals("Invoice", saved.getTitle());
        verify(ocrJobs).publish(saved.getId(), saved.getObjectKey());
    }

    @Test
    void failedUploadSavesNoDocument() {
        doThrow(new StorageException("down", null)).when(storage).store(anyString(), any(), anyLong(), any());

        assertThrows(StorageException.class, () -> service.upload(alice, "Invoice", "invoice.pdf",
                "application/pdf", 3, InputStream.nullInputStream()));
        verify(documents, never()).save(any());
        verify(ocrJobs, never()).publish(any(), any());
    }

    @Test
    void findAllReturnsTheOwnersDocuments() {
        List<Document> owned = List.of(document(alice));
        when(documents.findByOwner(alice)).thenReturn(owned);

        assertEquals(owned, service.findAll(alice));
    }

    @Test
    void anotherUsersDocumentIsNotFound() {
        Document doc = document(bob);
        when(documents.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThrows(DocumentNotFoundException.class, () -> service.find(alice, doc.getId()));
    }

    @Test
    void downloadLoadsTheBlobByObjectKey() {
        InputStream content = InputStream.nullInputStream();
        when(storage.load("key")).thenReturn(content);

        assertSame(content, service.download(document(alice)));
    }

    @Test
    void renameChangesTheTitle() {
        Document doc = document(alice);
        when(documents.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(documents.save(doc)).thenReturn(doc);

        assertEquals("Receipt", service.rename(alice, doc.getId(), "Receipt").getTitle());
    }

    @Test
    void deleteRemovesTheDocumentFromGroupsTheRowAndTheBlob() {
        Document doc = document(alice);
        DocumentGroup group = new DocumentGroup("Invoices", alice);
        group.addDocument(doc);
        when(documents.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(groups.findByDocumentsContaining(doc)).thenReturn(List.of(group));

        service.delete(alice, doc.getId());

        assertFalse(group.getDocuments().contains(doc));
        verify(documents).delete(doc);
        verify(storage).delete("key");
    }

    @Test
    void deletingAnotherUsersDocumentTouchesNothing() {
        Document doc = document(bob);
        when(documents.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThrows(DocumentNotFoundException.class, () -> service.delete(alice, doc.getId()));
        verify(documents, never()).delete(any());
        verify(storage, never()).delete(any());
    }

    private static User user(String name) {
        User user = new User(name, "hash");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private static Document document(User owner) {
        Document doc = new Document("Invoice", "invoice.pdf", "application/pdf", 10, "key");
        ReflectionTestUtils.setField(doc, "id", UUID.randomUUID());
        doc.setOwner(owner);
        return doc;
    }
}
