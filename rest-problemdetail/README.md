# rest-problemdetail

A small orders REST API in `App.java` whose errors are all RFC 9457 `application/problem+json`, built on `ProblemDetail` and `ResponseEntityExceptionHandler`, with `ProblemTest`.

## Goal

Return every error, including Bean Validation failures, in one standard problem format instead of Spring's default error JSON.

## Run it

```bash
./mvnw -q test
```

Expected: `ProblemTest` passes with no failures; the log has a `Request method 'DELETE' is not supported` warning from the 405 case.

## What it proves

- An invalid `POST /orders` body (`@NotBlank customer`, `@Min(1) quantity`) returns a 400 problem document.
- `GET /orders/{id}` for an unknown id throws `OrderNotFoundException`, mapped by `ProblemAdvice` to a 404 problem with a custom `type` URI and an `orderId` extension member.
- A framework error such as 405 is also a problem document, because `ProblemAdvice` extends `ResponseEntityExceptionHandler`.

## Trade-offs

- One advice class gives a uniform error contract, but the custom `type` URIs (here `https://example.com/problems/order-not-found`) must be owned and documented.
- The default validation response does not list individual field errors; add them as a property if clients need them.
- Orders are held in an in-memory map and nothing is persisted.

## When not to use it

- Public APIs that already have a frozen error format.
- Non-HTTP boundaries such as messaging, where problem documents do not apply.
