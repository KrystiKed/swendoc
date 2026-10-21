package at.technikum.swendoc.DocumentGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.technikum.swendoc.document.Document;
import at.technikum.swendoc.document.DocumentNotFoundException;
import at.technikum.swendoc.document.DocumentRepository;
import at.technikum.swendoc.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DocumentGroupServiceTest {

    @Mock
    private DocumentGroupRepository groups;

    @Mock
    private DocumentRepository documents;

    private DocumentGroupService service;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        service = new DocumentGroupServiceImpl(groups, documents);
        alice = user("alice");
        bob = user("bob");
    }

    @Test
    void addDocumentPutsTheOwnersDocumentIntoTheGroup() {
        DocumentGroup group = new DocumentGroup("Invoices", alice);
        Document doc = document(alice);
        when(groups.findById(any())).thenReturn(Optional.of(group));
        when(documents.findById(any())).thenReturn(Optional.of(doc));
        when(groups.save(group)).thenReturn(group);

        DocumentGroup result = service.addDocument(alice, UUID.randomUUID(), UUID.randomUUID());

        assertTrue(result.getDocuments().contains(doc));
    }

    @Test
    void anotherUsersDocumentCannotBeAdded() {
        when(groups.findById(any())).thenReturn(Optional.of(new DocumentGroup("Invoices", alice)));
        when(documents.findById(any())).thenReturn(Optional.of(document(bob)));

        assertThrows(DocumentNotFoundException.class,
                () -> service.addDocument(alice, UUID.randomUUID(), UUID.randomUUID()));
        verify(groups, never()).save(any());
    }

    @Test
    void anotherUsersGroupIsReportedAsMissing() {
        when(groups.findById(any())).thenReturn(Optional.of(new DocumentGroup("Bob's", bob)));

        assertThrows(DocumentGroupNotFoundException.class, () -> service.find(alice, UUID.randomUUID()));
    }

    @Test
    void createStoresTheGroupForItsOwner() {
        when(groups.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentGroup created = service.create(alice, "Contracts");

        assertEquals("Contracts", created.getName());
        assertEquals(alice, created.getOwner());
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
