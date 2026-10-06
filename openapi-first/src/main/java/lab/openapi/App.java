package lab.openapi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lab.openapi.api.OrdersApi;
import lab.openapi.model.NewOrder;
import lab.openapi.model.Order;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

/** Only the behaviour is hand-written: routes, status codes and validation come from the generated OrdersApi. */
@RestController
class OrdersController implements OrdersApi {
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    @Override
    public ResponseEntity<Order> createOrder(NewOrder newOrder) {
        var order = new Order(ids.incrementAndGet(), newOrder.getCustomer(), newOrder.getQuantity());
        orders.put(order.getId(), order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Override
    public ResponseEntity<Order> getOrder(Long id) {
        var order = orders.get(id);
        return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order);
    }
}
