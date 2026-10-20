package at.technikum.swendoc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import at.technikum.swendoc.document.DocumentMapperImpl;
import at.technikum.swendoc.user.InvalidCredentialsException;
import at.technikum.swendoc.user.User;
import at.technikum.swendoc.user.UserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import at.technikum.swendoc.validation.ValidatorFactory;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = at.technikum.swendoc.document.DocumentController.class)
@Import({ValidatorFactory.class, DocumentMapperImpl.class})
class DocumentControllerTest {

    private static final String TOKEN = "Bearer t";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService service;

    @MockitoBean
    private UserService users;

    private final User alice = new User("alice", "x");

    @BeforeEach
    void setUp() {
        when(users.authenticate(TOKEN)).thenReturn(alice);
        when(users.authenticate(null)).thenThrow(new InvalidCredentialsException());
    }

    @Test
    void uploadStoresFileForTheCallerAndReturnsCreated() throws Exception {
        var file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", "pdf-bytes".getBytes());
        when(service.upload(eq(alice), eq("Invoice"), eq("invoice.pdf"), eq("application/pdf"), anyLong(), any()))
                .thenReturn(new Document("Invoice", "invoice.pdf", "application/pdf", 9, "key"));

        mockMvc.perform(multipart("/api/docs").file(file).param("title", "Invoice").header("Authorization", TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Invoice"))
                .andExpect(jsonPath("$.filename").value("invoice.pdf"));
    }

    @Test
    void uploadRejectsEmptyFile() throws Exception {
        var empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/docs").file(empty).param("title", "Empty").header("Authorization", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("file"))
                .andExpect(jsonPath("$.errors[0].constraint").value("FileSize"));
    }

    @Test
    void uploadWithoutTitleOrFileReportsBothFields() throws Exception {
        mockMvc.perform(multipart("/api/docs").header("Authorization", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("file"))
                .andExpect(jsonPath("$.errors[0].message").value("Please choose a file."))
                .andExpect(jsonPath("$.errors[1].field").value("title"));
    }

    @Test
    void everyEndpointRequiresASession() throws Exception {
        UUID id = UUID.randomUUID();
        var file = new MockMultipartFile("file", "x.pdf", "application/pdf", "x".getBytes());

        mockMvc.perform(get("/api/docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/docs/" + id)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/docs/" + id + "/content")).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/docs").file(file).param("title", "X")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/docs/" + id)).andExpect(status().isUnauthorized());
        verify(service, never()).delete(any(), any());
    }

    @Test
    void unknownOrForeignIdIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.find(alice, id)).thenThrow(new DocumentNotFoundException(id));

        mockMvc.perform(get("/api/docs/" + id).header("Authorization", TOKEN)).andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/docs/" + id).header("Authorization", TOKEN)).andExpect(status().isNoContent());

        verify(service).delete(alice, id);
    }
}
