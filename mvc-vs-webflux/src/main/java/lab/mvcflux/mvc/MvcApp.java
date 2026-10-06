package lab.mvcflux.mvc;

import java.util.Map;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Servlet stack: one platform thread per in-flight request, blocking is the normal style. */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class MvcApp {
    @RestController
    static class SlowController {
        @GetMapping("/slow")
        Map<String, String> slow() throws InterruptedException {
            Thread.sleep(200); // stands in for a blocking JDBC/HTTP call
            return Map.of("stack", "mvc");
        }
    }
}
