package lab.modulith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import lab.modulith.inventory.StockReservations;
import lab.modulith.orders.OrderService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootTest
class ModulithTest {
    @Autowired OrderService orders;
    @Autowired StockReservations stock;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void structureIsValid() {
        var modules = ApplicationModules.of(App.class);
        modules.verify(); // fails on cycles or access to another module's internal types
        assertTrue(modules.getModuleByName("orders").isPresent());
        assertTrue(modules.getModuleByName("inventory").isPresent());
    }

    @Test
    void eventCrossesTheModuleBoundary() {
        orders.place("sku-1", 3);
        orders.place("sku-1", 2);
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> stock.reservedFor("sku-1") == 5);
        // delivered publications are marked completed in the event publication registry
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> jdbc.queryForObject(
            "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL", Integer.class) == 0);
        assertEquals(5, stock.reservedFor("sku-1"));
    }
}
