package com.css.challenge.storage;

import com.css.challenge.Main;
import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Storage {
    private static final Logger LOGGER = LoggerFactory.getLogger(Storage.class);

    private int capacity;
    private ConcurrentLinkedQueue<Order> orders;

    public Storage(int capacity) {
        this.capacity = capacity;
        this.orders = new ConcurrentLinkedQueue<>();
    }

    public boolean addOrder(Order order) {
        if (orders.size() < capacity) {
            orders.add(order);
            return true;
        }
        return false;
    }

    public boolean moveOrder(Order order){
        if(orders.size() < capacity){
            int orderFreshness = order.getFreshness();
            order.setFreshness(orderFreshness/2);
            orders.add(order);
            return true;
        }
        return false;
    }

    public boolean removeOrder(String orderId) {
        return orders.removeIf(order -> order.getId().equals(orderId));
    }

    public boolean isFull() {
        return orders.size() >= capacity;
    }

    public boolean pickupOrder(String orderId, List<Action> actions){
        Order orderToPick = orders.stream().filter(order -> order.getId().equals(orderId)).findFirst().orElse(null);
        if(orderToPick == null){
            return false;
        }
        long now = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
        long orderTimestamp = orderToPick.getTimestamp();
        float timeLapsAfterPlacingOrder = (now - orderTimestamp)/Long.parseLong(Main.get("epoch.to.seconds"));
        if(timeLapsAfterPlacingOrder > 8){
            if(removeOrder(orderId)){
                long orderDiscardTimestamp = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
                actions.add(new Action(orderDiscardTimestamp, orderId, Action.DISCARD));
            }
            return false;
        }
        if(timeLapsAfterPlacingOrder >= 4 && timeLapsAfterPlacingOrder < 8){
            return removeOrder(orderId);
        }
        return false;
    }

}
