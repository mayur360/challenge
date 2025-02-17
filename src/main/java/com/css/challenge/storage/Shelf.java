package com.css.challenge.storage;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.management.OrderManagementSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

public class Shelf extends Storage {
    private static final Logger LOGGER = LoggerFactory.getLogger(Shelf.class);

    private PriorityQueue<Order> freshnessQueue;


    public Shelf(int capacity) {
        super(capacity);
        freshnessQueue = new PriorityQueue<>((o1, o2) -> {
            long freshness1 = o1.getFreshness() - (System.currentTimeMillis() - o1.getTimestamp()) / 1000;
            long freshness2 = o2.getFreshness() - (System.currentTimeMillis() - o2.getTimestamp()) / 1000;
            return Long.compare(freshness1, freshness2);

        });


    }

    @Override
    public boolean addOrder(Order order) {
        if (super.addOrder(order)) {
            freshnessQueue.add(order);
            return true;
        }
        return false;
    }

    @Override
    public boolean moveOrder(Order order) {
        if(super.moveOrder(order)){
            freshnessQueue.add(order);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeOrder(String orderId) {
        boolean removed = super.removeOrder(orderId);
        if (removed) {
            freshnessQueue.removeIf(order -> order.getId().equals(orderId));
        }
        return removed;
    }

    public Order getOrderToDiscard() {
        return freshnessQueue.poll();
    }

    public Order getLeastFreshColdOrder() {
        return freshnessQueue.stream()
                .filter(order -> "cold".equalsIgnoreCase(order.getTemp()))
                .findFirst()
                .orElse(null);
    }

    public Order getLeastFreshHotOrder() {
        return freshnessQueue.stream()
                .filter(order -> "hot".equalsIgnoreCase(order.getTemp()))
                .findFirst()
                .orElse(null);
    }

    public boolean pickupOrder(String orderId, List<Action> actions){
        LOGGER.info(orderId+" in Shelf PickUpOrder");
        Order orderToPick = freshnessQueue.stream().filter(order -> order.getId().equals(orderId)).findFirst().orElse(null);
        LOGGER.info(orderToPick+" in Shelf PickUpOrder");
        if(orderToPick != null){
            long now = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
            long orderTimestamp = orderToPick.getTimestamp();
            LOGGER.info("Shelf : now "+now+" & orderTimestamp "+orderTimestamp);
            long timeLapsAfterPlacingOrder = (now - orderTimestamp)/1000000;
            LOGGER.info("Shelf : timeLapsAfterPlacingOrder value is "+timeLapsAfterPlacingOrder);
            if(timeLapsAfterPlacingOrder >= 8){
                if(removeOrder(orderId)){
                    LOGGER.info(orderId+" is discarded in Shelf");
                    actions.add(new Action(Instant.now(), orderId, Action.DISCARD));
                };
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
