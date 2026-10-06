# retry-and-session

One small MVC app in `App.java` with two patterns: Spring Retry around a flaky `PricingClient`, and Spring Session JDBC for a visit counter, tested by `RetrySessionTest`.

## Goal

Show `@Retryable` with an `@Recover` fallback, and HTTP session state stored in a database table instead of server memory.

## Run it

```bash
./mvnw -q test
```

Expected: `RetrySessionTest` passes with no failures and prints nothing notable.

## What it proves

- A dependency that fails twice is called three times and `/price` still succeeds (`maxAttempts = 3`, 10 ms backoff).
- When it keeps failing, the `@Recover` method answers after exactly three attempts.
- A counter incremented over two `/visit` requests with the `SESSION` cookie is stored in Spring Session's JDBC tables (`spring.session.jdbc.initialize-schema=always`), so another instance sharing that database could serve it.

## Trade-offs

- Retries multiply load on a struggling dependency; real systems also need growing backoff with jitter, idempotency and a circuit breaker.
- `@Retryable` works through a proxy, so self-invocation is not retried, and tests must use accessor methods on the bean, not fields.
- A JDBC session costs a database round trip per request; Redis is faster for hot sessions. In-memory H2 only shares state inside one JVM, so this shows the mechanism, not real clustering.
- It uses Spring Retry from Boot's dependency management rather than the retry support built into newer Spring Framework versions.

## When not to use it

- Retrying non-idempotent calls without a deduplication key.
- Stateless token-based APIs, where a server-side session is unnecessary.
