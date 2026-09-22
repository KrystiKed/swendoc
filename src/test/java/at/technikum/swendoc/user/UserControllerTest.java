package at.technikum.swendoc.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService service;

    @Test
    void registerReturnsCreatedWithoutThePasswordHash() throws Exception {
        when(service.register(any(), any())).thenReturn(new User("alice", "c2FsdA==:aGFzaA=="));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerRejectsAShortPassword() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithWrongCredentialsIsUnauthorized() throws Exception {
        when(service.login(any(), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong-horse\"}"))
                .andExpect(status().isUnauthorized());
    }
}
