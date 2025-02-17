package com.css.challenge.storage;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

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

    public ConcurrentLinkedQueue<Order> getOrders() {
        return orders;
    }

    public boolean pickupOrder(String orderId, List<Action> actions){
        LOGGER.info(orderId+" in Storage PickUpOrder");
        Order orderToPick = orders.stream().filter(order -> order.getId().equals(orderId)).findFirst().orElse(null);
        LOGGER.info(orderToPick+" in storage PickUpOrder");
        if(orderToPick != null){
            long now = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
            long orderTimestamp = orderToPick.getTimestamp();
            LOGGER.info("Storage : now "+now+" & orderTimestamp "+orderTimestamp);
            long timeLapsAfterPlacingOrder = (now - orderTimestamp)/1000000;
            LOGGER.info("Storage : timeLapsAfterPlacingOrder value is "+timeLapsAfterPlacingOrder);
            if(timeLapsAfterPlacingOrder >= 8){
                LOGGER.info(orderId+" is discarded in Shelf");
                if(removeOrder(orderId)){
                    actions.add(new Action(Instant.now(), orderId, Action.DISCARD));
                }
                return false;
            }
            if(timeLapsAfterPlacingOrder >= 4 && timeLapsAfterPlacingOrder < 8){
                LOGGER.info("Storage : Pick up order "+orderId);
                return removeOrder(orderId);
            }
        }
        return false;
    }

}
