package at.technikum.swendoc.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import at.technikum.swendoc.validation.ValidatorFactory;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@Import(ValidatorFactory.class)
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].constraint").value("Size"))
                .andExpect(jsonPath("$.errors[0].message").value("Must be between 8 and 128 characters long."));
    }

    @Test
    void validationMessagesFollowAcceptLanguage() throws Exception {
        mockMvc.perform(post("/users")
                        .header("Accept-Language", "de-AT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\" \",\"password\":\"correct-horse\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("username"))
                .andExpect(jsonPath("$.errors[0].message").value("Bitte füllen Sie dieses Feld aus."));
    }

    @Test
    void loginWithWrongCredentialsIsUnauthorized() throws Exception {
        when(service.login(any(), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong-horse\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheLoggedInUserWithoutThePasswordHash() throws Exception {
        when(service.authenticate("Bearer t")).thenReturn(new User("alice", "c2FsdA==:aGFzaA=="));

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meWithoutASessionIsUnauthorized() throws Exception {
        when(service.authenticate(null)).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordReturnsNoContent() throws Exception {
        User alice = new User("alice", "x");
        when(service.authenticate("Bearer t")).thenReturn(alice);

        mockMvc.perform(put("/users/me/password").header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isNoContent());
        verify(service).changePassword(alice, "old-password", "new-password");
    }

    @Test
    void changePasswordRejectsAShortNewPasswordBeforeTouchingTheUser() throws Exception {
        when(service.authenticate("Bearer t")).thenReturn(new User("alice", "x"));

        mockMvc.perform(put("/users/me/password").header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"));
        verify(service, never()).changePassword(any(), any(), any());
    }

    @Test
    void changePasswordWithAWrongCurrentPasswordIsForbidden() throws Exception {
        when(service.authenticate("Bearer t")).thenReturn(new User("alice", "x"));
        when(service.changePassword(any(), any(), any())).thenThrow(new WrongPasswordException());

        mockMvc.perform(put("/users/me/password").header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isForbidden());
    }
}
