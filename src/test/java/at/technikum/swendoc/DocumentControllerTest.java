package at.technikum.swendoc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.technikum.swendoc.document.Document;
import at.technikum.swendoc.document.DocumentNotFoundException;
import at.technikum.swendoc.document.DocumentService;
import at.technikum.swendoc.user.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import at.technikum.swendoc.validation.ValidatorFactory;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = at.technikum.swendoc.document.DocumentController.class)
@Import(ValidatorFactory.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService service;

    @MockitoBean
    private UserService users;

    @Test
    void uploadStoresFileAndReturnsCreated() throws Exception {
        var file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", "pdf-bytes".getBytes());
        when(service.upload(eq("Invoice"), any(), any()))
                .thenReturn(new Document("Invoice", "invoice.pdf", "application/pdf", 9, "key"));

        mockMvc.perform(multipart("/docs").file(file).param("title", "Invoice"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Invoice"))
                .andExpect(jsonPath("$.filename").value("invoice.pdf"));
    }

    @Test
    void uploadRejectsEmptyFile() throws Exception {
        var empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/docs").file(empty).param("title", "Empty"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("file"))
                .andExpect(jsonPath("$.errors[0].constraint").value("FileSize"));
    }

    @Test
    void uploadWithoutTitleOrFileReportsBothFields() throws Exception {
        mockMvc.perform(multipart("/docs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("file"))
                .andExpect(jsonPath("$.errors[0].message").value("Please choose a file."))
                .andExpect(jsonPath("$.errors[1].field").value("title"));
    }

    @Test
    void unknownIdIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.find(id)).thenThrow(new DocumentNotFoundException(id));

        mockMvc.perform(get("/docs/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/docs/" + id)).andExpect(status().isNoContent());

        verify(service).delete(id);
    }
}
