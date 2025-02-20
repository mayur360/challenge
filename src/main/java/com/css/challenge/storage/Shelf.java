package com.css.challenge.storage;

import com.css.challenge.Main;
import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.PriorityQueue;

public class Shelf extends Storage {
    private static final Logger LOGGER = LoggerFactory.getLogger(Shelf.class);

    private PriorityQueue<Order> freshnessQueue;


    public Shelf(int capacity) {
        super(capacity);
        freshnessQueue = new PriorityQueue<>((o1, o2) -> {
            long now = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
            long freshness1 = o1.getFreshness() - (now - o1.getTimestamp()) / Long.parseLong(Main.get("epoch.to.seconds"));
            long freshness2 = o2.getFreshness() - (now - o2.getTimestamp()) / Long.parseLong(Main.get("epoch.to.seconds"));
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
            Order orderToRemoveFromShelf = freshnessQueue.stream().filter(order -> order.getId().equals(orderId)).findFirst().orElse(null);
            if(orderToRemoveFromShelf != null){
                freshnessQueue.remove(orderToRemoveFromShelf);
            }
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
        Order orderToPick = freshnessQueue.stream().filter(order -> order.getId().equals(orderId)).findFirst().orElse(null);
        if(orderToPick == null){
            return false;
        }

        long now = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
        long orderTimestamp = orderToPick.getTimestamp();
        long timeLapsAfterPlacingOrder = (now - orderTimestamp)/Long.parseLong(Main.get("epoch.to.seconds"));
        if(timeLapsAfterPlacingOrder > 8.0){
            if(removeOrder(orderId)){
                long orderDiscardTimestamp = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
                actions.add(new Action(orderDiscardTimestamp, orderId, Action.DISCARD));
            }
            return false;
        }
        if(timeLapsAfterPlacingOrder >= 4.0 && timeLapsAfterPlacingOrder <= 8.0){
            return removeOrder(orderId);
        }
        return false;
    }
}
