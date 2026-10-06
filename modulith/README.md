# modulith

A Spring Boot app split into two Spring Modulith modules, `orders` and `inventory`, connected only by the `OrderPlaced` event, with `ModulithTest` checking the structure.

## Goal

Show module boundaries inside one application, enforced by a test, with asynchronous event delivery between the modules.

## Run it

```bash
./mvnw -q test
```

Expected: `ModulithTest` passes with no failures; output is startup logs and JVM agent warnings from Mockito.

## What it proves

- `ApplicationModules.of(App.class).verify()` passes: no cycles and no access to another module's internal packages.
- `OrderService.place` publishes `OrderPlaced`; `StockReservations` in `inventory` receives it through `@ApplicationModuleListener` (asynchronous, after commit) and the reserved quantity shows up.
- The event is stored in the `EVENT_PUBLICATION` table, in the same transaction as the publisher, and marked completed after delivery.

## Trade-offs

- Persistence is the JDBC starter with H2 rather than JPA, to get the event registry without an ORM; the schema is created by `spring.modulith.events.jdbc.schema-initialization.enabled=true`.
- Boundaries are enforced by tests, not by the compiler; it is still one deployable and one JVM.
- Asynchronous delivery means eventual consistency between modules, so tests have to wait (Awaitility).
- Inventory state is an in-memory map, enough for the example only.

## When not to use it

- Tiny apps whose package structure is not yet a problem.
- When modules already need independent deployment or scaling; split them into services instead.
