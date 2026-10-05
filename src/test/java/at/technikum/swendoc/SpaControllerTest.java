package at.technikum.swendoc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SpaController.class)
class SpaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clientRoutesForwardToTheAngularShell() throws Exception {
        mockMvc.perform(get("/documents")).andExpect(forwardedUrl("/index.html"));
        mockMvc.perform(get("/login")).andExpect(forwardedUrl("/index.html"));
    }
}
