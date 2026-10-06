package lab.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class BatchTest {
    @Autowired JobLauncher launcher;
    @Autowired Job importJob;
    @Autowired JdbcTemplate jdbc;
    @Autowired AtomicBoolean crashSwitch;

    List<Long> ids() { return jdbc.queryForList("SELECT id FROM processed ORDER BY id", Long.class); }

    @Test
    void skipsBadRowsThenRestartsFromTheFailedChunk() throws Exception {
        crashSwitch.set(true);
        var params = new JobParametersBuilder().addString("run", "1").toJobParameters();

        var first = launcher.run(importJob, params);
        assertEquals(BatchStatus.FAILED, first.getStatus());
        // chunk [1,2] committed; chunk [5,6] was rolled back despite its inserts (3 and 4 were skipped).
        assertEquals(List.of(1L, 2L), ids());

        var second = launcher.run(importJob, params); // same parameters => restart of the failed instance
        assertEquals(BatchStatus.COMPLETED, second.getStatus());
        assertEquals(first.getJobId(), second.getJobId());
        assertEquals(List.of(1L, 2L, 5L, 6L, 7L), ids());

        var step = second.getStepExecutions().iterator().next();
        System.out.printf("RESULT restart: read=%d write=%d skip=%d (first run skips: %d)%n", step.getReadCount(),
            step.getWriteCount(), step.getSkipCount(), first.getStepExecutions().iterator().next().getSkipCount());
    }
}
