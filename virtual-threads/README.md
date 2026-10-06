# virtual-threads

A plain blocking MVC endpoint (`/slow`, `Thread.sleep(200)`) in `App.java`, and `VirtualThreadsTest`, which starts it twice with Tomcat limited to 10 threads: once on platform threads, once with virtual threads.

## Goal

Show that `spring.threads.virtual.enabled=true` removes the thread-pool limit on a blocking endpoint without any code change.

## Run it

```bash
./mvnw -q test | grep RESULT
```

Expected, from a run on a 16-core WSL2 machine (your numbers will differ):

```
RESULT platform(10 threads)=2062 ms {"virtual":false} | virtual=263 ms {"virtual":true}
```

## What it proves

- With 10 platform threads, 100 requests that each block for 200 ms take about 2060 ms, the expected 100 x 200 ms / 10 floor, and the handler reports `virtual:false`.
- With virtual threads the same burst takes about 260 ms, because the waits overlap, and the handler reports `virtual:true`.
- The only difference between the two runs is configuration; the controller is identical.

## Trade-offs

- Virtual threads help blocking, I/O-bound code; CPU-bound work gains nothing.
- On Java 21, `synchronized` around a blocking call can pin a carrier thread. The Tomcat limit is gone, so downstream limits (a database pool, for example) become the bottleneck and need their own bulkheads.
- Thread-locals and per-thread pooling assumptions need review.
- Compared with WebFlux (see `../mvc-vs-webflux`) you keep imperative code and plain stack traces.

## When not to use it

- CPU-bound services.
- Code that relies on a small thread pool as implicit rate limiting.
