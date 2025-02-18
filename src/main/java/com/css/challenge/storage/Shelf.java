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
import java.util.Queue;

public class Shelf extends Storage {
    private static final Logger LOGGER = LoggerFactory.getLogger(Shelf.class);

    private PriorityQueue<Order> freshnessQueue;


    public Shelf(int capacity) {
        super(capacity);
        freshnessQueue = new PriorityQueue<>((o1, o2) -> {
            long now = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
            long freshness1 = o1.getFreshness() - (now - o1.getTimestamp()) / 1000000;
            long freshness2 = o2.getFreshness() - (now - o2.getTimestamp()) / 1000000;
            return Long.compare(freshness1, freshness2);

        });


    }

    @Override
    public boolean addOrder(Order order) {
        LOGGER.info("adding order in shelf");
        if (super.addOrder(order)) {
            freshnessQueue.add(order);
            return true;
        }
        LOGGER.info("After moving order to shelf, check items in shelf "+checkItemsInShelf());
        return false;
    }

    @Override
    public boolean moveOrder(Order order) {
        LOGGER.info("Moving order in shelf");
        if(super.moveOrder(order)){
            freshnessQueue.add(order);
            return true;
        }
        LOGGER.info("After moving order to shelf, check items in shelf "+checkItemsInShelf());
        return false;
    }

    @Override
    public boolean removeOrder(String orderId) {
        boolean removed = super.removeOrder(orderId);
        LOGGER.info("Shelf: Items in Stotage "+super.checkOrdersInStorage());
        if (removed) {
            Order orderToRemoveFromShelf = freshnessQueue.stream().filter(order -> order.getId().equals(orderId)).findFirst().orElse(null);
            if(orderToRemoveFromShelf != null){
                freshnessQueue.remove(orderToRemoveFromShelf);
                LOGGER.info("Shelf : Least fresh order is removed");
            }
        }
        LOGGER.info("Items in Shelf: "+ checkItemsInShelf());
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
        if(orderToPick == null){
            return false;
        }

        long now = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
        long orderTimestamp = orderToPick.getTimestamp();
        LOGGER.info("Shelf : now "+now+" & orderTimestamp "+orderTimestamp);
        long timeLapsAfterPlacingOrder = (now - orderTimestamp)/1000000;
        LOGGER.info("Shelf : timeLapsAfterPlacingOrder value is "+timeLapsAfterPlacingOrder);
        if(timeLapsAfterPlacingOrder > 8.0){
            if(removeOrder(orderId)){
                LOGGER.info(orderId+" is discarded in Shelf");
                actions.add(new Action(Instant.now(), orderId, Action.DISCARD));
            }
            return false;
        }
        if(timeLapsAfterPlacingOrder >= 4.0 && timeLapsAfterPlacingOrder <= 8.0){
            LOGGER.info("Storage : Pick up order "+orderId);
            return removeOrder(orderId);
        }
        Main.orderQueue.add(orderToPick);
        return false;
    }

    public int checkItemsInShelf() {
        return freshnessQueue.size();
    }
}
