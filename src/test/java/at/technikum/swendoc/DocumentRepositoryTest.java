package at.technikum.swendoc;

import static org.assertj.core.api.Assertions.assertThat;

import at.technikum.swendoc.document.Document;
import at.technikum.swendoc.document.DocumentRepository;
import at.technikum.swendoc.document.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documents;

    @Test
    void findsOnlyDocumentsOfTheGivenType() {
        documents.save(new Document("Invoice", "invoice.pdf", "application/pdf", 1, "k1"));
        documents.save(new Document("Letter", "letter.docx", "application/octet-stream", 1, "k2"));
        documents.save(new Document("Budget", "budget.xlsx", "application/octet-stream", 1, "k3"));
        documents.save(new Document("Report", "report.pdf", "application/pdf", 1, "k4"));
        documents.save(new Document("Notes", "notes.txt", "text/plain", 1, "k5"));

        assertThat(documents.findByDocumentType(DocumentType.PDF))
                .extracting(Document::getTitle).containsExactlyInAnyOrder("Invoice", "Report");
        assertThat(documents.findByDocumentType(DocumentType.WORD))
                .extracting(Document::getTitle).containsExactly("Letter");
        assertThat(documents.findByDocumentType(DocumentType.EXCEL))
                .extracting(Document::getTitle).containsExactly("Budget");
    }
}
