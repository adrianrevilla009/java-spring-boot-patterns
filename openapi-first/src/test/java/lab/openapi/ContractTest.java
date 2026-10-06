package lab.openapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ContractTest {
    @Autowired MockMvc mvc;

    @Test
    void createThenFetch() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{\"customer\":\"ada\",\"quantity\":2}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/orders/1")).andExpect(jsonPath("$.customer").value("ada"));
        mvc.perform(get("/orders/99")).andExpect(status().isNotFound());
    }

    @Test
    void constraintsFromTheSpecAreEnforced() throws Exception {
        // quantity has "minimum: 1" in openapi.yaml; the generated model carries @Min(1)
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{\"customer\":\"ada\",\"quantity\":0}"))
            .andExpect(status().isBadRequest());
    }
}
