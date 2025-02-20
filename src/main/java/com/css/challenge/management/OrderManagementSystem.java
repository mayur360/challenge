package com.css.challenge.management;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.storage.Shelf;
import com.css.challenge.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class OrderManagementSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderManagementSystem.class);

    private Storage cooler;
    private Storage heater;
    private Shelf shelf;

    public OrderManagementSystem() {
        this.cooler = new Storage(6);
        this.heater = new Storage(6);
        this.shelf = new Shelf(12);
    }

    public synchronized void placeOrder(Order order, List<Action> actions) {
        LOGGER.info("Placing order : "+order.getId()+" & temp : "+order.getTemp());
        switch (order.getTemp()) {
            case "hot":
                handleHotOrders(order, actions);
                break;
            case "cold":
                handleColdOrders(order, actions);
                break;
            default:
                handleRoomOrders(order, actions );
                break;
        }
    }

    // Pickup logic
    public synchronized void pickupOrder(Order order, List<Action> actions) {
        LOGGER.info("Picking up : "+order.getId());
        String orderId = order.getId();

        switch (order.getTemp()) {
            case "hot":
                long hotOrderPickUpTime = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
                if(heater.pickupOrder(orderId, actions)){
                    actions.add(new Action(hotOrderPickUpTime, orderId, Action.PICKUP));
                    break;
                }
                if (shelf.pickupOrder(orderId, actions)) {
                    actions.add(new Action(hotOrderPickUpTime, orderId, Action.PICKUP));
                }
                break;
            case "cold":
                long coldOrderPickUpTime = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
                if(cooler.pickupOrder(orderId, actions)){
                    actions.add(new Action(coldOrderPickUpTime, orderId, Action.PICKUP));
                    break;
                }
                if (shelf.pickupOrder(orderId, actions)) {
                    actions.add(new Action(coldOrderPickUpTime, orderId, Action.PICKUP));
                }
                break;
            default:
                long shelfOrderPickUpTime = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
                if(shelf.pickupOrder(orderId, actions)){
                    actions.add(new Action(shelfOrderPickUpTime, orderId, Action.PICKUP));
                }
                break;
        }
    }

    public void handleHotOrders(Order order, List<Action> actions){
        long hotOrderPlacedTime = order.getTimestamp();
        if(heater.addOrder(order)){
            LOGGER.info("Hot Order place in heater");
            actions.add(new Action(hotOrderPlacedTime, order.getId(), Action.PLACE));
            return;
        }
        if(shelf.moveOrder(order)){
            LOGGER.info("Heater was full so Hot Order moved to Shelf ");
            actions.add(new Action(hotOrderPlacedTime, order.getId(), Action.PLACE));
            return;
        }
        LOGGER.info("Heater was full and Shelf is also full so we are checking space in cooler");
        long discardTimeStamp = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
        if(cooler.isFull()){
            LOGGER.info(" Cooler is full so removing least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                LOGGER.info("least fresh order discarded as heater, shelf & cooler, all are full");
                actions.add(new Action(discardTimeStamp, discardedOrder.getId(), Action.DISCARD));
            }
            if(shelf.moveOrder(order)){
                LOGGER.info(" Hot order moved to shelf with reduced freshness to half ");
                order.setTimestamp(Instant.now());
                actions.add(new Action(hotOrderPlacedTime, order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info("Cooler is not full so we will move cold order from shelf to cooler");
        Order coldOrder = shelf.getLeastFreshColdOrder();
        if(coldOrder == null){
            LOGGER.info(" Did not find cold order on shelf so discard least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();

            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                LOGGER.info(" Cooler has space but no cold order on shelf so discarding least fresh order ");
                actions.add(new Action(discardTimeStamp, discardedOrder.getId(), Action.DISCARD));
            }
            if (shelf.moveOrder(order)){
                LOGGER.info("Added new order to shelf after discarding old order from shelf as we didn't find cold order on shelf");
                order.setTimestamp(Instant.now());
                actions.add(new Action(hotOrderPlacedTime, order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(" We found cold order on shelf ");
        if (cooler.addOrder(coldOrder)){
            LOGGER.info("Cold order added in cooler and removed from shelf");
            shelf.removeOrder(coldOrder.getId());
            actions.add(new Action(discardTimeStamp, coldOrder.getId(), Action.MOVE));
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" New order added to shelf");
                order.setTimestamp(Instant.now());
                actions.add(new Action(hotOrderPlacedTime, order.getId(), Action.PLACE));
            }
        }

    }

    public void handleColdOrders(Order order, List<Action> actions){
        LOGGER.info("Cold order received Order id : "+order.getId());
        long coldOrderPlacedTime = order.getTimestamp();
        if(cooler.addOrder(order)){
            order.setTimestamp(Instant.now());
            actions.add(new Action(coldOrderPlacedTime, order.getId(), Action.PLACE));
            return;
        }
        if(shelf.moveOrder(order)){
            LOGGER.info("Cooler was full so Hot Order is moved to Shelf ");
            order.setTimestamp(Instant.now());
            actions.add(new Action(coldOrderPlacedTime, order.getId(), Action.PLACE));
            return;
        }
        LOGGER.info("Cooler was full and Shelf is also full so we are checking space in Heater");
        long discardTimeStamp = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
        if (heater.isFull()){
            LOGGER.info(order.getId()+" Heater is full so removing least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                LOGGER.info("Handling cold order : least fresh order discarded as heater, shelf & cooler, all are full");
                actions.add(new Action(discardTimeStamp, discardedOrder.getId(), Action.DISCARD));
            }
            if(shelf.moveOrder(order)){
                LOGGER.info(" Cold order moved to shelf with reduced freshness to half ");
                order.setTimestamp(Instant.now());
                actions.add(new Action(coldOrderPlacedTime, order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info("Heater is not full so we move hot order from shelf to cooler");
        Order hotOrder = shelf.getLeastFreshHotOrder();
        if(hotOrder == null){
            LOGGER.info(" Did not find Hold order on shelf so discard least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                LOGGER.info(" heater has space but no hot order on shelf so discarding least fresh order");
                actions.add(new Action(discardTimeStamp, discardedOrder.getId(), Action.DISCARD));
            }
            if (shelf.moveOrder(order)){
                LOGGER.info("Added new order to shelf after discarding old order from shelf as we didn't find hot order on shelf");
                order.setTimestamp(Instant.now());
                actions.add(new Action(coldOrderPlacedTime, order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(" We found hot order on shelf "+hotOrder);
        if (heater.addOrder(hotOrder)){
            LOGGER.info(hotOrder.getId()+" Hot order added in heater and removed from shelf");
            shelf.removeOrder(hotOrder.getId());
            actions.add(new Action(discardTimeStamp, hotOrder.getId(), Action.MOVE));
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" New order added to shelf and reduced freshness to half "+order.getFreshness());
                order.setTimestamp(Instant.now());
                actions.add(new Action(coldOrderPlacedTime, order.getId(), Action.PLACE));
            }
        }
    }

    public void handleRoomOrders(Order order, List<Action> actions){
        LOGGER.info("Order with Room temperature received");
        long shelfOrderPlacedTime = order.getTimestamp();
        if(shelf.addOrder(order)){
            LOGGER.info("Normal Order put on shelf");
            order.setTimestamp(Instant.now());
            actions.add(new Action(shelfOrderPlacedTime, order.getId(), Action.PLACE));
            return;
        }
        LOGGER.info("Shelf is full so checking space in heater and if heater doesn't has space then we will check in cooler");
        long discardTimeStamp = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
        if (!heater.isFull()){
            LOGGER.info("Heater has space");
            Order hotOrder = shelf.getLeastFreshHotOrder();
            if(hotOrder != null){
                LOGGER.info("We found a hot order on shelf, so moving it from shelf to heater");
                if (heater.addOrder(hotOrder)){
                    shelf.removeOrder(hotOrder.getId());
                    actions.add(new Action(discardTimeStamp, hotOrder.getId(), Action.MOVE));
                    if(shelf.moveOrder(order)){
                        order.setTimestamp(Instant.now());
                        actions.add(new Action(shelfOrderPlacedTime, order.getId(), Action.PLACE));
                        return;
                    }
                }
            }
        } else if (!cooler.isFull()) {
            LOGGER.info("Shelf was full, Heater was also full but cooler has space");
            Order coldOrder = shelf.getLeastFreshColdOrder();
            if(coldOrder != null){
                LOGGER.info("We found a cold order on shelf, so moving it from shelf to cooler");
                if (cooler.addOrder(coldOrder)){
                    shelf.removeOrder(coldOrder.getId());
                    actions.add(new Action(discardTimeStamp, coldOrder.getId(), Action.MOVE));
                    if(shelf.moveOrder(order)){
                        order.setTimestamp(Instant.now());
                        actions.add(new Action(shelfOrderPlacedTime, order.getId(), Action.PLACE));
                        return;
                    }
                }
            }
        }
        Order discardedOrder = shelf.getOrderToDiscard();
        if (discardedOrder != null) {
            shelf.removeOrder(discardedOrder.getId());
            actions.add(new Action(discardTimeStamp, discardedOrder.getId(), Action.DISCARD));
        }
        if (shelf.moveOrder(order)){
            order.setTimestamp(Instant.now());
            actions.add(new Action(shelfOrderPlacedTime, order.getId(), Action.PLACE)); //Commented
        }
    }

}

