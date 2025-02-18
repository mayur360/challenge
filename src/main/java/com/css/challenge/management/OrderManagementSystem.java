package com.css.challenge.management;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.storage.Shelf;
import com.css.challenge.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderManagementSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderManagementSystem.class);

    private Storage cooler;
    private Storage heater;
    private Shelf shelf;
    private List<String> actionLog;

    public OrderManagementSystem() {
        this.cooler = new Storage(6);
        this.heater = new Storage(6);
        this.shelf = new Shelf(12);
        this.actionLog = new ArrayList<>();
    }

    public synchronized void placeOrder(Order order, List<Action> actions) {
        // Try to place in ideal storage
        //Handle Hot order
        Instant orderPlaceTime = Instant.now();
        switch (order.getTemp()) {
            case "hot":
                handleHotOrders(order, actions, orderPlaceTime);
                break;
            case "cold":
                handleColdOrders(order, actions, orderPlaceTime);
                break;
            default:
                handleRoomOrders(order, actions, orderPlaceTime);
                break;
        }
    }

    // Pickup logic
    public synchronized void pickupOrder(Order order, List<Action> actions) {
        LOGGER.info("Thread name "+Thread.currentThread().getName());
        String pickupThreadName = Thread.currentThread().getName();
        String orderId = order.getId();

        switch (order.getTemp()) {
            case "hot":
                if(heater.pickupOrder(orderId, actions)){
                    actions.add(new Action(Instant.now(), orderId, Action.PICKUP));
                    return;
                }
                if (shelf.pickupOrder(orderId, actions)) {
                    actions.add(new Action(Instant.now(), orderId, Action.PICKUP));
                    return;
                }
                break;
            case "cold":
                if(cooler.pickupOrder(orderId, actions)){
                    actions.add(new Action(Instant.now(), orderId, Action.PICKUP));
                    return;
                }
                if (shelf.pickupOrder(orderId, actions)) {
                    actions.add(new Action(Instant.now(), orderId, Action.PICKUP));
                    return;
                }
                break;
            default:
                if(shelf.pickupOrder(orderId, actions)){
                actions.add(new Action(Instant.now(), orderId, Action.PICKUP));
            }
            break;
        }
    }

    private void logAction(String action, String orderId) {

        actionLog.add(String.format("%d: %s %s", System.currentTimeMillis(), action, orderId));
    }

    public List<String> getActionLog() {
        return actionLog;
    }

    public void handleHotOrders(Order order, List<Action> actions, Instant now){
        LOGGER.info("Hot order received Order id : "+order.getId());
        if(heater.addOrder(order)){
            LOGGER.info("Hot Order place in heater");
            order.setTimestamp(now);
            actions.add(new Action(now, order.getId(), Action.PLACE));
            return;
        }
        if(shelf.moveOrder(order)){
            LOGGER.info("Heater was full so Hot Order is moved to Shelf "+order.getId());
            boolean exists = actions.stream()
                    .anyMatch(action -> order.getId().equals(action.getId()));
            LOGGER.info("Is this order already exist in Action list : "+exists);
            if(exists){
                actions.add(new Action(Instant.now(), order.getId(), Action.MOVE));
                return;
            }
            order.setTimestamp(Instant.now());
            actions.add(new Action(now, order.getId(), Action.PLACE));
            return;
        }
        LOGGER.info("Heater was full and Shelf is also full so we are checking space in cooler");
        if(cooler.isFull()){
            LOGGER.info(order.getId()+" Cooler is full so removing least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                LOGGER.info(" least fresh order discarded as heater, shelf & cooler, all are full");
                actions.add(new Action(Instant.now(), discardedOrder.getId(), Action.DISCARD));
            }
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" Hot order moved to shelf with reduced freshness to half = "+ order.getFreshness());
                order.setTimestamp(Instant.now());
                actions.add(new Action(now, order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(order.getId()+" Cooler is not full so we move cold order from shelf to cooler");
        Order coldOrder = shelf.getLeastFreshColdOrder();
        if(coldOrder == null){
            LOGGER.info(" Did not find cold order on shelf so discard least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();

            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                actions.add(new Action(Instant.now(), discardedOrder.getId(), Action.DISCARD));
                logAction("discard", discardedOrder.getId());
            }
            if (shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" Added new order to shelf after discrading old order from shelf as we didnt find cold order on shelf");
                order.setTimestamp(Instant.now());
                actions.add(new Action(now, order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(" We found cold order on shelf "+coldOrder);
        if (cooler.addOrder(coldOrder)){
            LOGGER.info(coldOrder.getId()+" Cold order added in cooler and removed from shelf");
            shelf.removeOrder(coldOrder.getId());
            actions.add(new Action(Instant.now(), coldOrder.getId(), Action.MOVE));
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" New order added to shelf");
                order.setTimestamp(Instant.now());
                actions.add(new Action(now, order.getId(), Action.PLACE));
            }
        }

    }

    public void handleColdOrders(Order order, List<Action> actions, Instant now){
        LOGGER.info("Cold order received Order id : "+order.getId());
        if(cooler.addOrder(order)){
            order.setTimestamp(Instant.now());
            actions.add(new Action(now, order.getId(), Action.PLACE));
            return;
        }
        if(shelf.moveOrder(order)){
            LOGGER.info("Cooler was full so Hot Order is moved to Shelf "+order.getId());
            boolean exists = actions.stream()
                    .anyMatch(action -> order.getId().equals(action.getId()));
            if(!exists){
                order.setTimestamp(Instant.now());
                actions.add(new Action(now, order.getId(), Action.PLACE));
                return;
            }
            actions.add(new Action(Instant.now(), order.getId(), Action.MOVE));
            return;
        }
        LOGGER.info("Cooler was full and Shelf is also full so we are checking space in Heater");
        if (heater.isFull()){
            LOGGER.info(order.getId()+" Heater is full so removing least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                LOGGER.info(" least fresh order discarded as heater, shelf & cooler, all are full");
                actions.add(new Action(Instant.now(), discardedOrder.getId(), Action.DISCARD));
                logAction("discard", discardedOrder.getId());
            }
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" Cold order moved to shelf with reduced freshness to half = "+ order.getFreshness());
                order.setTimestamp(Instant.now());
                actions.add(new Action(now, order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(order.getId()+" Heater is not full so we move hot order from shelf to cooler");
        Order hotOrder = shelf.getLeastFreshHotOrder();
        if(hotOrder == null){
            LOGGER.info(" Did not find Hold order on shelf so discard least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                actions.add(new Action(Instant.now(), discardedOrder.getId(), Action.DISCARD));
                logAction("discard", discardedOrder.getId());
            }
            if (shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" Added new order to shelf after discrading old order from shelf as we didnt find hot order on shelf with half of freshness");
                order.setTimestamp(Instant.now());
                actions.add(new Action(now, order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(" We found hot order on shelf "+hotOrder);
        if (heater.addOrder(hotOrder)){
            LOGGER.info(hotOrder.getId()+" Hot order added in heater and removed from shelf");
            shelf.removeOrder(hotOrder.getId());
            actions.add(new Action(Instant.now(), hotOrder.getId(), Action.MOVE));
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" New order added to shelf and reduced freshness to half "+order.getFreshness());
                order.setTimestamp(Instant.now());
                actions.add(new Action(now, order.getId(), Action.PLACE));
            }
        }
    }

    public void handleRoomOrders(Order order, List<Action> actions, Instant now){
        LOGGER.info("Order with Room temperature received");
        if(shelf.addOrder(order)){
            LOGGER.info("Normal Order put on shelf");
            order.setTimestamp(Instant.now());
            actions.add(new Action(now, order.getId(), Action.PLACE));
            return;
        }
        LOGGER.info("Shelf is full so checking space in heater and if heater doesn't has space then we will check in cooler");
        if (!heater.isFull()){
            LOGGER.info("Heater has space");
            Order hotOrder = shelf.getLeastFreshHotOrder();
            if(hotOrder != null){
                LOGGER.info("We found a hot order on shelf, so moving it from shelf to heater");
                if (heater.addOrder(hotOrder)){
                    shelf.removeOrder(hotOrder.getId());
                    //updateAction(actions, hotOrder.getId(), Action.MOVE);
                    actions.add(new Action(Instant.now(), hotOrder.getId(), Action.MOVE));
                    if(shelf.moveOrder(order)){
                        order.setTimestamp(Instant.now());
                        //updateAction(actions, order.getId(), Action.PLACE);
                        actions.add(new Action(now, order.getId(), Action.PLACE));
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
                    //updateAction(actions, coldOrder.getId(), Action.MOVE);
                    actions.add(new Action(Instant.now(), coldOrder.getId(), Action.MOVE));
                    if(shelf.moveOrder(order)){
                        order.setTimestamp(Instant.now());
                        //updateAction(actions, order.getId(), Action.PLACE);
                        actions.add(new Action(now, order.getId(), Action.PLACE));
                        return;
                    }
                }
            }
        }
        Order discardedOrder = shelf.getOrderToDiscard();
        if (discardedOrder != null) {
            shelf.removeOrder(discardedOrder.getId());
            //updateAction(actions, discardedOrder.getId(), Action.DISCARD);
            actions.add(new Action(Instant.now(), discardedOrder.getId(), Action.DISCARD));
            logAction("discard", discardedOrder.getId());
        }
        if (shelf.moveOrder(order)){
            order.setTimestamp(Instant.now());
            //updateAction(actions, order.getId(), Action.PLACE);
            actions.add(new Action(now, order.getId(), Action.PLACE));
        }
    }

}

