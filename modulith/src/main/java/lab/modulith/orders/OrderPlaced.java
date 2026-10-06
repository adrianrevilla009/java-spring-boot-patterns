package lab.modulith.orders;

/** Published API of the orders module: other modules may depend on this event, not on orders' internals. */
public record OrderPlaced(long orderId, String sku, int quantity) {}
