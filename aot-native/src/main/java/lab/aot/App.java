package lab.aot;

import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Deliberately ordinary: AOT/native needs no code changes for plain MVC + records. */
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    record Order(long id, String customer) {}

    @RestController
    static class Api {
        @GetMapping("/orders")
        List<Order> orders() { return List.of(new Order(1, "ada"), new Order(2, "bob")); }
    }
}
