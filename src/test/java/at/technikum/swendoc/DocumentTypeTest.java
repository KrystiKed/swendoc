package at.technikum.swendoc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.technikum.swendoc.document.DocumentType;
import at.technikum.swendoc.document.UnknownDocumentTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DocumentTypeTest {

    @ParameterizedTest
    @CsvSource({
            "a.pdf,  application/pdf,                                                           PDF",
            "a.doc,  application/msword,                                                        WORD",
            "a.docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document,   WORD",
            "a.xls,  application/vnd.ms-excel,                                                  EXCEL",
            "a.xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,         EXCEL",
            // browsers often send octet-stream for Office files; the extension decides then
            "a.DOCX, application/octet-stream,                                                  WORD",
            "a.xlsx, application/octet-stream,                                                  EXCEL",
            "a.pdf,  application/octet-stream,                                                  PDF",
    })
    void detectsType(String filename, String contentType, DocumentType expected) {
        assertThat(DocumentType.detect(filename, contentType)).contains(expected);
    }

    @Test
    void otherFilesHaveNoType() {
        assertThat(DocumentType.detect("notes.txt", "text/plain")).isEmpty();
        assertThat(DocumentType.detect(null, null)).isEmpty();
    }

    @Test
    void pathValueIsCaseInsensitive() {
        assertThat(DocumentType.fromPath("word")).isEqualTo(DocumentType.WORD);
        assertThat(DocumentType.fromPath("Excel")).isEqualTo(DocumentType.EXCEL);
        assertThatThrownBy(() -> DocumentType.fromPath("pptx"))
                .isInstanceOf(UnknownDocumentTypeException.class);
    }
}
