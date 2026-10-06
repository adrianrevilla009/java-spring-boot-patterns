# statemachine

An order lifecycle modelled as a Spring Statemachine in `OrderStateMachine.java`, with four JUnit tests in `OrderStateMachineTest.java`.

## Goal

Make the order lifecycle (`CREATED -> PAID -> SHIPPED`, with `CANCEL` allowed until shipped) an explicit machine, so illegal transitions are rejected by the model instead of by scattered `if` statements.

## Run it

```bash
mvn -q test
```

Not run end to end. As committed, `pom.xml` declares the `<parent>` block twice, so Maven stops with `Non-parseable POM ... Duplicated tag: 'parent'`, and the folder has no Maven wrapper. Removing the duplicate block (lines 10-15) is the only change needed before the command can run; the tests below have never been executed.

## What it proves

By reading the code and tests, they are meant to show:
- The happy path `PAY` with an `amount` header, then `SHIP`, ends in the terminal `SHIPPED` state and `isComplete()` is true.
- A guard in `OrderStateMachine.java` rejects `PAY` when the `amount` header is missing or zero.
- `SHIP` before `PAY` and `CANCEL` after `SHIPPED` are denied, and `CANCEL` from `PAID` is accepted.

## Trade-offs

- The machine is built with `StateMachineBuilder` instead of Boot auto-configuration, which keeps the example small and the tests plain JUnit, but it does not show Spring wiring.
- One machine instance holds one order's state; storing and restoring it per order needs the separate persist module, which is not used here.
- Only `spring-statemachine-core` 4.0.0 is used, with the Boot 3.4.5 parent for test dependency management.

## When not to use it

- For four states and a few transitions, an `enum` with a transition table is simpler.
- For long-running, human-driven workflows, a BPMN engine is a better fit.
