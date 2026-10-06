package lab.retrysession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RetrySessionTest {
    @Autowired MockMvc mvc;
    @Autowired PricingClient pricing;
    @Autowired JdbcTemplate jdbc;

    @Test
    void transientFailuresAreRetriedUntilSuccess() throws Exception {
        pricing.reset(2);
        mvc.perform(get("/price")).andExpect(jsonPath("$.price").value(42));
        assertEquals(3, pricing.calls());
    }

    @Test
    void exhaustedRetriesFallBackToRecover() throws Exception {
        pricing.reset(10);
        mvc.perform(get("/price")).andExpect(jsonPath("$.price").value(-1));
        assertEquals(3, pricing.calls()); // maxAttempts, not more
    }

    @Test
    void sessionStateIsStoredInTheDatabase() throws Exception {
        var first = mvc.perform(get("/visit")).andExpect(jsonPath("$.visits").value(1)).andReturn();
        var cookie = first.getResponse().getCookie("SESSION");
        // MockMvc without a servlet container: Spring Session's filter issued the SESSION cookie
        var again = mvc.perform(get("/visit").cookie(cookie)).andExpect(jsonPath("$.visits").value(2)).andReturn();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES WHERE ATTRIBUTE_NAME = 'visits'", Integer.class)
            .intValue());
    }
}
