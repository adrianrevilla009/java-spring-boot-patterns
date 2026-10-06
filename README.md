# java-spring-boot-patterns

Ten small Spring Boot 3.4 examples, each isolating one pattern (error format, threading model, data access, batch, modules, state machine, retry, native image, contract-first API) around a tiny Orders domain.

## What is inside

| Folder | What it shows | Run |
| --- | --- | --- |
| [`rest-problemdetail`](./rest-problemdetail) | RFC 9457 problem+json errors, including validation failures | `./mvnw -q test` |
| [`mvc-vs-webflux`](./mvc-vs-webflux) | Same blocking endpoint on MVC and WebFlux, timed with 100 concurrent requests | `./mvnw -q test \| grep RESULT` |
| [`virtual-threads`](./virtual-threads) | A blocking MVC endpoint with and without virtual threads, Tomcat capped at 10 threads | `./mvnw -q test \| grep RESULT` |
| [`data-jpa-jdbc-jooq`](./data-jpa-jdbc-jooq) | One query through JPA, JdbcClient and jOOQ on H2 | `./mvnw -q test` |
| [`batch`](./batch) | Spring Batch chunking, skip and restart after a simulated crash | `./mvnw -q test` |
| [`modulith`](./modulith) | Two Spring Modulith modules linked by an event, with a structure verification test | `./mvnw -q test` |
| [`statemachine`](./statemachine) | Order lifecycle as a Spring Statemachine with a guard (not runnable as committed, see its README) | `mvn -q test` |
| [`retry-and-session`](./retry-and-session) | Spring Retry with a fallback, and HTTP sessions stored in a database | `./mvnw -q test` |
| [`aot-native`](./aot-native) | Startup and memory script for JVM, Spring AOT and GraalVM native builds | `./measure.sh jvm` |
| [`openapi-first`](./openapi-first) | Controller interface and DTOs generated from an OpenAPI file | `./mvnw -q test` |

## Prerequisites

- Java 21 (the GraalVM `native-image` tool is only needed for the native part of `aot-native`).
- Network access to Maven Central on the first build. Each folder has its own Maven wrapper, except `statemachine`.
- No Docker and no external database: everything uses in-memory H2.

## How to read it

Folders are independent; start with `rest-problemdetail` for the smallest one, then `mvc-vs-webflux` and `virtual-threads` for the two folders that print timings.
