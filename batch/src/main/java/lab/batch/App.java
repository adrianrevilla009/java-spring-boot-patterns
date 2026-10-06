package lab.batch;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
public class App {
    record Order(long id, String customer, BigDecimal total) {}

    /** Test switch: when armed, the writer fails once after inserting the chunk holding order 5 or 6. */
    final AtomicBoolean crashOnce = new AtomicBoolean(false);

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    AtomicBoolean crashSwitch() { return crashOnce; }

    /** FlatFileItemReader is restartable: it stores the line position in the step execution context. */
    @Bean
    FlatFileItemReader<Order> reader() {
        return new FlatFileItemReaderBuilder<Order>().name("orderReader")
            .resource(new ClassPathResource("orders.csv")).linesToSkip(1).delimited().names("id", "customer", "total")
            .fieldSetMapper(fs -> new Order(fs.readLong("id"), fs.readString("customer"), fs.readBigDecimal("total")))
            .build();
    }

    @Bean
    ItemProcessor<Order, Order> validate() {
        return o -> {
            if (o.customer().equals("BAD")) throw new IllegalArgumentException("rejected order " + o.id());
            return o;
        };
    }

    @Bean
    ItemWriter<Order> writer(JdbcTemplate jdbc) {
        return chunk -> {
            for (Order o : chunk) jdbc.update("INSERT INTO processed VALUES (?, ?, ?)", o.id(), o.customer(), o.total());
            boolean hasFive = chunk.getItems().stream().anyMatch(o -> o.id() == 5 || o.id() == 6);
            if (hasFive && crashOnce.compareAndSet(true, false)) throw new IllegalStateException("simulated crash");
        };
    }

    @Bean
    Step importStep(JobRepository repo, PlatformTransactionManager tx, FlatFileItemReader<Order> reader,
                    ItemProcessor<Order, Order> validate, ItemWriter<Order> writer) {
        return new StepBuilder("importStep", repo).<Order, Order>chunk(2, tx)
            .reader(reader).processor(validate).writer(writer)
            .faultTolerant()
            .skip(FlatFileParseException.class).skip(IllegalArgumentException.class).skipLimit(5)
            .build();
    }

    @Bean
    Job importJob(JobRepository repo, Step importStep) {
        return new JobBuilder("importJob", repo).start(importStep).build();
    }
}
