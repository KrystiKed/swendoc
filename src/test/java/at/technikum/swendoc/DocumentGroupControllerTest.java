package at.technikum.swendoc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.technikum.swendoc.document.Document;
import at.technikum.swendoc.document.DocumentGroupController;
import at.technikum.swendoc.document.DocumentService;
import at.technikum.swendoc.document.DocumentType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DocumentGroupController.class)
class DocumentGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService service;

    @Test
    void listsTheThreeTypes() throws Exception {
        mockMvc.perform(get("/documents/group"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("WORD"))
                .andExpect(jsonPath("$[1]").value("PDF"))
                .andExpect(jsonPath("$[2]").value("EXCEL"));
    }

    @Test
    void listsDocumentsOfType() throws Exception {
        when(service.findByType(DocumentType.PDF)).thenReturn(List.of(
                new Document("Invoice 1", "a.pdf", "application/pdf", 1, "k1"),
                new Document("Invoice 2", "b.pdf", "application/pdf", 2, "k2")));

        mockMvc.perform(get("/documents/group/pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Invoice 1"))
                .andExpect(jsonPath("$[0].documentType").value("PDF"));
    }

    @Test
    void typeInPathIsCaseInsensitive() throws Exception {
        when(service.findByType(DocumentType.EXCEL)).thenReturn(List.of());

        mockMvc.perform(get("/documents/group/Excel")).andExpect(status().isOk());
    }

    @Test
    void unknownTypeIsNotFound() throws Exception {
        mockMvc.perform(get("/documents/group/powerpoint")).andExpect(status().isNotFound());
    }
}
