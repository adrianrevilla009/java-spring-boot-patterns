package lab.mvcflux;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lab.mvcflux.flux.FluxApp;
import lab.mvcflux.mvc.MvcApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Fires the same burst of 100 requests (200 ms of "blocking work" each) at three configurations. */
class StackComparisonTest {
    static final int REQUESTS = 100;

    static ConfigurableApplicationContext start(Class<?> app, WebApplicationType type) {
        return new SpringApplicationBuilder(app).web(type).properties("server.port=0", "spring.main.banner-mode=off",
            "logging.level.root=WARN").run();
    }

    static long burstMillis(ConfigurableApplicationContext ctx, String path) {
        var uri = URI.create("http://localhost:" + ctx.getEnvironment().getProperty("local.server.port") + path);
        var client = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).build();
        client.sendAsync(req, HttpResponse.BodyHandlers.discarding()).join(); // warm-up
        long t0 = System.nanoTime();
        List<CompletableFuture<HttpResponse<Void>>> all = java.util.stream.IntStream.range(0, REQUESTS)
            .mapToObj(i -> client.sendAsync(req, HttpResponse.BodyHandlers.<Void>discarding())).toList();
        all.forEach(CompletableFuture::join);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("RESULT %-28s %5d ms for %d requests%n", path + " @" + ctx.getId().split("-")[0], ms, REQUESTS);
        return ms;
    }

    @Test
    void blockingOnTheEventLoopIsTheWorstCase() {
        long mvc, blocking, reactive, offloaded;
        try (var ctx = start(MvcApp.class, WebApplicationType.SERVLET)) { mvc = burstMillis(ctx, "/slow"); }
        try (var ctx = start(FluxApp.class, WebApplicationType.REACTIVE)) {
            blocking = burstMillis(ctx, "/slow");
            reactive = burstMillis(ctx, "/slow-reactive");
            offloaded = burstMillis(ctx, "/slow-offloaded");
        }
        System.out.printf("RESULT mvc=%d flux-blocking=%d flux-reactive=%d flux-offloaded=%d (ms), cores=%d%n",
            mvc, blocking, reactive, offloaded, Runtime.getRuntime().availableProcessors());
        // 100 x 200 ms serialised over N event-loop threads is far slower than any non-blocking variant.
        assertTrue(blocking > reactive * 1.5, "blocking the event loop should be clearly slower than Mono.delay");
        assertTrue(blocking > mvc * 1.5, "MVC's 200 threads absorb the same blocking call better");
    }
}
