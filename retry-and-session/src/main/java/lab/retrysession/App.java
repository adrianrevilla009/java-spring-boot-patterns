package lab.retrysession;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableRetry
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

/** A dependency that fails transiently: the first {@code failures} calls throw. */
@Service
class PricingClient {
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int failures = 2;

    // Accessors, not fields: the bean is a CGLIB proxy, so field access from outside would hit the proxy.
    void reset(int failures) { this.failures = failures; calls.set(0); }
    int calls() { return calls.get(); }

    @Retryable(retryFor = IllegalStateException.class, maxAttempts = 3, backoff = @Backoff(delay = 10))
    int price(String sku) {
        if (calls.incrementAndGet() <= failures) throw new IllegalStateException("pricing unavailable");
        return 42;
    }

    /** Called when attempts are exhausted. */
    @Recover
    int fallback(IllegalStateException e, String sku) {
        return -1;
    }
}

@RestController
class Api {
    private final PricingClient pricing;
    Api(PricingClient pricing) { this.pricing = pricing; }

    @GetMapping("/price")
    Map<String, Integer> price() { return Map.of("price", pricing.price("sku-1")); }

    /** The counter lives in the HTTP session, which Spring Session stores in the database. */
    @GetMapping("/visit")
    Map<String, Integer> visit(HttpSession session) {
        int n = (session.getAttribute("visits") instanceof Integer v ? v : 0) + 1;
        session.setAttribute("visits", n);
        return Map.of("visits", n);
    }
}
