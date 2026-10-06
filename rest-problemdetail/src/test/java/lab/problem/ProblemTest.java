package lab.problem;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemTest {
    @Autowired MockMvc mvc;

    @Test
    void validationFailureIsProblemJson() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"\",\"quantity\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void missingOrderIsCustomProblem() throws Exception {
        mvc.perform(get("/orders/42"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.type").value("https://example.com/problems/order-not-found"))
            .andExpect(jsonPath("$.orderId").value(42));
    }

    @Test
    void wrongMethodIsProblemToo() throws Exception {
        mvc.perform(delete("/orders/1"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentType("application/problem+json"));
    }
}
