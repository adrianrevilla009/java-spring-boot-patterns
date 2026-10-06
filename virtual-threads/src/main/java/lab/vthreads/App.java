package lab.vthreads;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Plain blocking MVC endpoint. Whether it runs on a platform or virtual thread is pure configuration. */
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @RestController
    static class SlowController {
        @GetMapping("/slow")
        Map<String, Object> slow() throws InterruptedException {
            Thread.sleep(200); // stands in for a blocking JDBC/HTTP call
            return Map.of("virtual", Thread.currentThread().isVirtual());
        }
    }
}
