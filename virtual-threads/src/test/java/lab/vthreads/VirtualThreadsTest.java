package lab.vthreads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Same app, same endpoint, Tomcat capped at 10 threads: only the virtual-thread switch differs. */
class VirtualThreadsTest {
    static final int REQUESTS = 100;

    static ConfigurableApplicationContext start(boolean virtual) {
        return new SpringApplicationBuilder(App.class).properties("server.port=0", "spring.main.banner-mode=off",
            "logging.level.root=WARN", "server.tomcat.threads.max=10",
            "spring.threads.virtual.enabled=" + virtual).run();
    }

    record Result(long millis, String body) {}

    static Result burst(ConfigurableApplicationContext ctx) {
        var uri = URI.create("http://localhost:" + ctx.getEnvironment().getProperty("local.server.port") + "/slow");
        var client = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).build();
        client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).join(); // warm-up
        long t0 = System.nanoTime();
        List<CompletableFuture<HttpResponse<String>>> all = IntStream.range(0, REQUESTS)
            .mapToObj(i -> client.sendAsync(req, HttpResponse.BodyHandlers.ofString())).toList();
        var last = all.stream().map(CompletableFuture::join).reduce((a, b) -> b).orElseThrow();
        return new Result((System.nanoTime() - t0) / 1_000_000, last.body());
    }

    @Test
    void virtualThreadsRemoveThePoolCeiling() {
        Result platform, virtual;
        try (var ctx = start(false)) { platform = burst(ctx); }
        try (var ctx = start(true)) { virtual = burst(ctx); }
        System.out.printf("RESULT platform(10 threads)=%d ms %s | virtual=%d ms %s%n",
            platform.millis(), platform.body(), virtual.millis(), virtual.body());
        assertEquals("{\"virtual\":false}", platform.body());
        assertEquals("{\"virtual\":true}", virtual.body());
        // 100 x 200 ms over 10 threads needs >= 2 s; virtual threads overlap all waits.
        assertTrue(platform.millis() >= 2000);
        assertTrue(virtual.millis() < platform.millis() / 2);
    }
}
