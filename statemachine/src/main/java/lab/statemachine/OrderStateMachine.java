package lab.statemachine;

import java.util.EnumSet;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import reactor.core.publisher.Mono;

/** Order lifecycle: CREATED -PAY-> PAID -SHIP-> SHIPPED, CANCEL allowed until shipped. */
public final class OrderStateMachine {
    public enum State { CREATED, PAID, SHIPPED, CANCELLED }
    public enum Event { PAY, SHIP, CANCEL }

    private OrderStateMachine() {}

    public static StateMachine<State, Event> create() {
        try {
            var b = StateMachineBuilder.<State, Event>builder();
            b.configureStates().withStates().initial(State.CREATED).states(EnumSet.allOf(State.class))
                .end(State.SHIPPED).end(State.CANCELLED);
            b.configureTransitions()
                // guard: a payment needs a positive "amount" header
                .withExternal().source(State.CREATED).target(State.PAID).event(Event.PAY)
                    .guard(ctx -> ctx.getMessageHeaders().get("amount", Integer.class) != null
                        && ctx.getMessageHeaders().get("amount", Integer.class) > 0).and()
                .withExternal().source(State.PAID).target(State.SHIPPED).event(Event.SHIP).and()
                .withExternal().source(State.CREATED).target(State.CANCELLED).event(Event.CANCEL).and()
                .withExternal().source(State.PAID).target(State.CANCELLED).event(Event.CANCEL);
            var sm = b.build();
            sm.startReactively().block();
            return sm;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Returns true when the machine accepted (took) the transition. */
    public static boolean send(StateMachine<State, Event> sm, Event event, Integer amount) {
        var msg = MessageBuilder.withPayload(event);
        if (amount != null) msg.setHeader("amount", amount);
        var results = sm.sendEvent(Mono.just(msg.build())).collectList().block();
        return results != null && results.stream()
            .allMatch(r -> r.getResultType() == org.springframework.statemachine.StateMachineEventResult.ResultType.ACCEPTED);
    }
}
