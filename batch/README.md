# batch

A Spring Batch job in `App.java` that imports `orders.csv` into an H2 table in chunks of 2, with skip rules and a test that crashes and restarts it.

## Goal

Show chunked processing, skipping bad records, and restarting a failed job from the last committed chunk.

## Run it

```bash
./mvnw -q test
```

Expected: the test passes. The log shows an `ERROR ... Encountered an error executing step` line (the simulated crash), one job ending `FAILED`, a second launch with the same parameters ending `COMPLETED`, and `RESULT restart: read=4 write=3 skip=2 (first run skips: 2)`.

## What it proves

- Chunking: reader, processor and writer commit every 2 items in one transaction.
- Skip: the unparsable row (`not-a-number`, id 3) and the row rejected by the processor (customer `BAD`, id 4) are skipped, not fatal (`skipLimit(5)`).
- Restart: a writer crash on the chunk holding rows 5 and 6 fails the job. Relaunching with identical parameters resumes the same job instance, rows 1 and 2 stay committed, the failed chunk is redone, and the table ends with each valid order once (1, 2, 5, 6, 7).

## Trade-offs

- The crash is injected with an `AtomicBoolean` bean so the test is deterministic. A real crash would be a killed JVM, which the same metadata tables handle, but that is not exercised.
- Batch metadata lives in in-memory H2, so a restart only works within one JVM. A real deployment needs a persistent database.
- Skip counts carry over on restart, so the second run reports the first run's skips too.
- Chunk size trades commit overhead against the amount of work redone after a rollback.

## When not to use it

- One-shot scripts or small data sets that fit in a single transaction.
- Streaming or event-driven processing, where a consumer fits better than a scheduled job.
