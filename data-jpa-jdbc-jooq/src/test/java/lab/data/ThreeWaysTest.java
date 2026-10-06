package lab.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ThreeWaysTest {
    @Autowired JpaOrders jpa;
    @Autowired JdbcOrders jdbc;
    @Autowired JooqOrders jooq;

    @Test
    void allThreeReturnTheSameAnswer() {
        var expected = List.of(new BigDecimal("25.50"), new BigDecimal("10.00"));
        var viaJpa = jpa.findByCustomerOrderByTotalDesc("ada").stream().map(o -> o.total).toList();
        assertEquals(expected, viaJpa);
        assertEquals(expected, jdbc.totals("ada"));
        assertEquals(expected, jooq.totals("ada"));
    }

    @Test
    @Transactional
    void jpaWritesAreVisibleToJooqInTheSameTransaction() {
        jpa.saveAndFlush(new OrderEntity("cy", new BigDecimal("3.00")));
        assertEquals(List.of(new BigDecimal("3.00")), jooq.totals("cy"));
    }
}
