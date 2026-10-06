package lab.mvcflux.flux;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Reactive stack: a handful of event-loop threads, so blocking one of them stalls many requests. */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class FluxApp {
    /** Tomcat is on the classpath for the MVC half, so pin Netty explicitly for the reactive half. */
    @Bean
    NettyReactiveWebServerFactory nettyFactory() {
        return new NettyReactiveWebServerFactory();
    }

    @RestController
    static class SlowController {
        /** Anti-pattern: blocks a Netty event-loop thread. */
        @GetMapping("/slow")
        Map<String, String> blocking() throws InterruptedException {
            Thread.sleep(200);
            return Map.of("stack", "webflux-blocking");
        }

        /** Non-blocking delay: no thread is held while waiting. */
        @GetMapping("/slow-reactive")
        Mono<Map<String, String>> reactive() {
            return Mono.delay(Duration.ofMillis(200)).map(t -> Map.of("stack", "webflux-reactive"));
        }

        /** Escape hatch when a blocking library is unavoidable: move it off the event loop. */
        @GetMapping("/slow-offloaded")
        Mono<Map<String, String>> offloaded() {
            return Mono.fromCallable(() -> {
                Thread.sleep(200);
                return Map.of("stack", "webflux-offloaded");
            }).subscribeOn(Schedulers.boundedElastic());
        }
    }
}
