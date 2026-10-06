# data-jpa-jdbc-jooq

One H2 `orders` table queried three ways in `App.java`: a Spring Data JPA repository, `JdbcClient`, and jOOQ.

## Goal

Ask the same question (totals for one customer, biggest first) with JPA, plain JDBC and jOOQ, so the code and the control each gives can be compared side by side.

## Run it

```bash
./mvnw -q test
```

Expected: `ThreeWaysTest` passes with no failures; the output is mostly the jOOQ banner and startup logs.

## What it proves

- `JpaOrders`, `JdbcOrders` and `JooqOrders` return the same results for the seed data in `schema.sql` (ada: 25.50 then 10.00).
- A JPA write inside a test transaction is visible to a jOOQ query, because both use the same connection and transaction.
- Code size differs: JPA is one derived-query method, JDBC is a SQL string with a type, jOOQ is a DSL chain.

## Trade-offs

- jOOQ uses name-based fields (`DSL.field(DSL.name("TOTAL"))`) instead of generated classes, to avoid a code-generation step. That gives up compile-time column checking, which is jOOQ's main benefit.
- JPA hides the SQL (and N+1 risks); JDBC has nothing to maintain but no compile-time help; jOOQ adds a dependency and, with generation, a build step.
- H2 stores unquoted identifiers in upper case, hence the upper-case names in the jOOQ code.

## When not to use it

- Do not mix all three in a real service without a reason: JPA suits aggregate-style CRUD, jOOQ or JDBC suit reporting queries.
- It is not a performance comparison; there is no benchmark.
