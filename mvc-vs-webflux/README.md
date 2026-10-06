# mvc-vs-webflux

Two apps in one project, `MvcApp` (Tomcat) and `FluxApp` (Netty), serving the same 200 ms blocking endpoint, plus `StackComparisonTest`, which times a burst of 100 requests against each.

## Goal

Turn the cost of blocking on a WebFlux event loop into a number, and show the two usual fixes.

## Run it

```bash
./mvnw -q test | grep RESULT
```

Expected, from a run on a 16-core WSL2 machine (your numbers will differ):

```
RESULT mvc=342 flux-blocking=1472 flux-reactive=299 flux-offloaded=311 (ms), cores=16
```

## What it proves

- MVC with `Thread.sleep(200)` finishes 100 requests in about 340 ms, because Tomcat has 200 threads by default.
- WebFlux with the same `Thread.sleep(200)` on the event loop takes about 1470 ms, because only a few Netty threads serve all requests.
- WebFlux with `Mono.delay` (about 300 ms) or with the blocking call moved to `Schedulers.boundedElastic()` (about 310 ms) is as fast as MVC.

## Trade-offs

- Both stacks run in one JVM with separate contexts and random ports; `FluxApp` pins `NettyReactiveWebServerFactory` because Tomcat, needed by the MVC half, would otherwise be picked.
- The test asserts a relative gap (blocking slower than 1.5 times the others), which depends on core count; on a machine with 100 or more cores the gap shrinks.
- WebFlux wins on memory per idle connection and on streaming, not on the latency of a blocking workload.

## When not to use it

- As a benchmark: it is one burst, not a load test.
- When your whole stack is blocking (JDBC); use MVC, ideally with virtual threads (see `../virtual-threads`).
