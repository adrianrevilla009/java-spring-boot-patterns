package lab.problem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

record OrderRequest(@NotBlank String customer, @Min(1) int quantity) {}

class OrderNotFoundException extends RuntimeException {
    final long id;
    OrderNotFoundException(long id) { super("Order " + id + " not found"); this.id = id; }
}

@RestController
@RequestMapping("/orders")
class OrderController {
    private final Map<Long, OrderRequest> orders = new ConcurrentHashMap<>();

    @PostMapping
    ResponseEntity<Void> create(@Valid @RequestBody OrderRequest request) {
        long id = orders.size() + 1L;
        orders.put(id, request);
        return ResponseEntity.created(URI.create("/orders/" + id)).build();
    }

    @GetMapping("/{id}")
    OrderRequest get(@PathVariable long id) {
        var order = orders.get(id);
        if (order == null) throw new OrderNotFoundException(id);
        return order;
    }
}

/** Extends the base advice, so every Spring MVC exception (incl. validation) is already RFC 9457. */
@RestControllerAdvice
class ProblemAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException e) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        pd.setType(URI.create("https://example.com/problems/order-not-found"));
        pd.setTitle("Order not found");
        pd.setProperty("orderId", e.id);
        return pd;
    }
}
