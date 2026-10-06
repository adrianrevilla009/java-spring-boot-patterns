# aot-native

A plain Spring Boot MVC app (`App.java`, one `/orders` endpoint) plus `measure.sh`, which reports startup time and resident memory for JVM, Spring AOT and native runs.

## Goal

Show that an ordinary Spring Boot app needs no code changes for AOT or native image, and give one script that measures all three modes the same way.

## Run it

```bash
./mvnw -q test                                                  # app test on the JVM
./mvnw -q -Pnative -DskipTests -DskipNativeBuild=true package   # jar with Spring AOT output
./measure.sh jvm                                                # prints RESULT jvm: Started App in N seconds | RSS M MB
./measure.sh aot                                                # same jar with -Dspring.aot.enabled=true
./mvnw -Pnative -DskipTests native:compile && ./measure.sh native   # needs GraalVM 21+ with native-image on PATH
```

Expected: the test passes, and the package step leaves `target/aot-native-1.0.0.jar` and generated sources under `target/spring-aot`. These two steps were run.

Not run end to end: `measure.sh` and the native build. No GraalVM `native-image` is installed on the machine used here, so there are no measured startup or memory figures in this repo.

## What it proves

- The app test passes and the AOT processing step generates bean definitions into `target/spring-aot` without any change to `App.java`.
- `measure.sh` takes the `Started App in ... seconds` log line and `VmRSS` from `/proc/<pid>/status`, so all three modes are read the same way.
- The server port is random (`server.port=0`), so repeated runs do not clash.

## Trade-offs

- Native images start much faster and use less memory, at the price of long builds, closed-world limits on reflection and proxies, and lower peak throughput than a warmed-up JIT. That is the usual expectation, not something measured here.
- `measure.sh` reads `/proc`, so it works on Linux only, and it measures a single start, not an average.
- The `native` profile comes from the Spring Boot 3.4.5 parent, which requires Java 21.

## When not to use it

- Long-running services where startup time does not matter and peak throughput does.
- Apps that rely on runtime reflection, dynamic proxies or classpath scanning without native hints.
