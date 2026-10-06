package lab.modulith.inventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lab.modulith.orders.OrderPlaced;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Reacts to orders asynchronously; inventory depends on orders' event only, never the reverse. */
@Component
public class StockReservations {
    private final Map<String, Integer> reserved = new ConcurrentHashMap<>();

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        reserved.merge(event.sku(), event.quantity(), Integer::sum);
    }

    public int reservedFor(String sku) { return reserved.getOrDefault(sku, 0); }
}
