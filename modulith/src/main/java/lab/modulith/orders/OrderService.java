package lab.modulith.orders;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final ApplicationEventPublisher events;
    private final AtomicLong ids = new AtomicLong();

    OrderService(ApplicationEventPublisher events) { this.events = events; }

    /** The event is stored in the same transaction and delivered to other modules after commit. */
    @Transactional
    public long place(String sku, int quantity) {
        long id = ids.incrementAndGet();
        events.publishEvent(new OrderPlaced(id, sku, quantity));
        return id;
    }
}
