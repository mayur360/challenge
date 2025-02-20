import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.management.OrderManagementSystem;
import com.css.challenge.storage.Shelf;
import com.css.challenge.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderManagementTest {

    private OrderManagementSystem oms;

    @Mock
    private Storage cooler;

    @Mock
    private Storage heater;

    @Mock
    private Shelf shelf;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        oms = new OrderManagementSystem();
    }

    @Test
    void testPlaceHotOrder_WhenHeaterHasSpace() {
        Order order = new Order("1", "Pizza", "hot", 50);
        order.setTimestamp(Instant.now());
        List<Action> actions = new ArrayList<>();

        when(heater.addOrder(order)).thenReturn(true);

        oms.placeOrder(order, actions);

        assertEquals(1, actions.size());
        assertEquals(Action.PLACE, actions.get(0).getAction());
    }

    @Test
    void testPlaceHotOrder_WhenHeaterIsFull_AndShelfHasSpace() {
        Order order = new Order("2", "Soup", "hot", 60);
        order.setTimestamp(Instant.now());
        List<Action> actions = new ArrayList<>();

        when(heater.addOrder(order)).thenReturn(false);
        when(shelf.moveOrder(order)).thenReturn(true);

        oms.placeOrder(order, actions);

        assertEquals(1, actions.size());
        assertEquals(Action.PLACE, actions.get(0).getAction());
    }

    @Test
    void testPlaceColdOrder_WhenCoolerHasSpace() {
        Order order = new Order("3", "Ice Cream", "cold", 80);
        order.setTimestamp(Instant.now());
        List<Action> actions = new ArrayList<>();

        when(cooler.addOrder(order)).thenReturn(true);

        oms.placeOrder(order, actions);

        assertEquals(1, actions.size());
        assertEquals(Action.PLACE, actions.get(0).getAction());
    }

    @Test
    void testPickupOrder_SuccessfullyPickedFromHeater() {
        Order order = new Order("4", "Coffee", "hot", 90);
        order.setTimestamp(Instant.now().minus(5, ChronoUnit.SECONDS));
        heater.addOrder(order);
        List<Action> actions = new ArrayList<>();

        actions.add(new Action(ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now()), "4", Action.PICKUP));
        when(heater.pickupOrder(order.getId(), actions)).thenReturn(true);

        oms.pickupOrder(order, actions);

        assertEquals(1, actions.size());
        assertEquals(Action.PICKUP, actions.get(0).getAction());
    }

    @Test
    void testPickupOrder_SuccessfullyPickedFromShelf() {
        Order order = new Order("5", "Burger", "cold", 40);
        order.setTimestamp(Instant.now());
        List<Action> actions = new ArrayList<>();

        actions.add(new Action(ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now()), "5", Action.PICKUP));
        when(cooler.pickupOrder(order.getId(), actions)).thenReturn(false);
        when(shelf.pickupOrder(order.getId(), actions)).thenReturn(true);

        oms.pickupOrder(order, actions);

        assertEquals(1, actions.size());
        assertEquals(Action.PICKUP, actions.get(0).getAction());
    }

    @Test
    void testHandleRoomOrders_SuccessfullyPlacedOnShelf() {
        Order order = new Order("6", "Sandwich", "room", 100);
        order.setTimestamp(Instant.now());
        List<Action> actions = new ArrayList<>();

        when(shelf.addOrder(order)).thenReturn(true);

        oms.handleRoomOrders(order, actions);

        assertEquals(1, actions.size());
        assertEquals(Action.PLACE, actions.get(0).getAction());
    }

}
