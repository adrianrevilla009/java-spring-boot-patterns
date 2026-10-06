package lab.statemachine;

import static lab.statemachine.OrderStateMachine.Event.*;
import static lab.statemachine.OrderStateMachine.State.*;
import static lab.statemachine.OrderStateMachine.send;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OrderStateMachineTest {
    @Test
    void happyPath() {
        var sm = OrderStateMachine.create();
        assertEquals(CREATED, sm.getState().getId());
        assertTrue(send(sm, PAY, 10));
        assertEquals(PAID, sm.getState().getId());
        assertTrue(send(sm, SHIP, null));
        assertEquals(SHIPPED, sm.getState().getId());
        assertTrue(sm.isComplete());
    }

    @Test
    void guardBlocksPaymentWithoutAmount() {
        var sm = OrderStateMachine.create();
        assertFalse(send(sm, PAY, null));
        assertFalse(send(sm, PAY, 0));
        assertEquals(CREATED, sm.getState().getId());
    }

    @Test
    void illegalTransitionsAreDenied() {
        var sm = OrderStateMachine.create();
        assertFalse(send(sm, SHIP, null)); // cannot ship before paying
        assertTrue(send(sm, PAY, 5));
        assertTrue(send(sm, SHIP, null));
        assertFalse(send(sm, CANCEL, null)); // cannot cancel after shipping
        assertEquals(SHIPPED, sm.getState().getId());
    }

    @Test
    void cancelFromPaid() {
        var sm = OrderStateMachine.create();
        send(sm, PAY, 5);
        assertTrue(send(sm, CANCEL, null));
        assertEquals(CANCELLED, sm.getState().getId());
    }
}
