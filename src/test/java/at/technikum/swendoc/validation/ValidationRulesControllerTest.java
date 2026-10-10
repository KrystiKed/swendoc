package at.technikum.swendoc.validation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ValidationRulesController.class)
class ValidationRulesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishesTheAnnotationRulesWithInterpolatedMessages() throws Exception {
        mockMvc.perform(get("/validation/credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password.length()").value(2))
                .andExpect(jsonPath("$.password[0].constraint").value("NotBlank"))
                .andExpect(jsonPath("$.password[1].constraint").value("Size"))
                .andExpect(jsonPath("$.password[1].attributes.min").value(8))
                .andExpect(jsonPath("$.password[1].attributes.max").value(128))
                .andExpect(jsonPath("$.password[1].message").value("Must be between 8 and 128 characters long."));
    }

    @Test
    void localizesMessagesAndCoversCustomConstraints() throws Exception {
        mockMvc.perform(get("/validation/upload").header("Accept-Language", "de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.file[0].constraint").value("NotNull"))
                .andExpect(jsonPath("$.file[1].constraint").value("FileSize"))
                .andExpect(jsonPath("$.file[1].attributes.maxMb").value(50))
                .andExpect(jsonPath("$.file[1].message").value("Die Datei darf nicht leer und höchstens 50 MB groß sein."));
    }

    @Test
    void unknownFormIsNotFound() throws Exception {
        mockMvc.perform(get("/validation/nope")).andExpect(status().isNotFound());
    }
}
