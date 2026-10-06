package lab.data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The same question ("totals for one customer, biggest first") asked through JPA, JDBC and jOOQ. */
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

@Entity
@Table(name = "orders")
class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    String customer;
    BigDecimal total;
    protected OrderEntity() {}
    OrderEntity(String customer, BigDecimal total) { this.customer = customer; this.total = total; }
}

/** 1. JPA: derived query, entity mapping, dirty checking; least SQL, least control. */
interface JpaOrders extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByCustomerOrderByTotalDesc(String customer);
}

/** 2. JDBC (JdbcClient): you own the SQL string, mapping is explicit. */
@Repository
class JdbcOrders {
    private final JdbcClient jdbc;
    JdbcOrders(JdbcClient jdbc) { this.jdbc = jdbc; }

    List<BigDecimal> totals(String customer) {
        return jdbc.sql("SELECT total FROM orders WHERE customer = ? ORDER BY total DESC")
            .param(customer).query(BigDecimal.class).list();
    }
}

/** 3. jOOQ: SQL as a type-checked-ish DSL. Plain names here; code generation would make them typed. */
@Repository
class JooqOrders {
    private final DSLContext dsl;
    JooqOrders(DSLContext dsl) { this.dsl = dsl; }

    List<BigDecimal> totals(String customer) {
        var total = DSL.field(DSL.name("TOTAL"), BigDecimal.class);
        return dsl.select(total).from(DSL.table(DSL.name("ORDERS")))
            .where(DSL.field(DSL.name("CUSTOMER"), String.class).eq(customer))
            .orderBy(total.desc()).fetch(total);
    }
}
