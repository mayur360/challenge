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
        if(order.getTemp().equals("hot")){
            handleHotOrders(order, actions);
        }

        if(order.getTemp().equals("cold")){
            handleColdOrders(order, actions);
        }

        if(order.getTemp().equals("room")){
            handleRoomOrders(order, actions);
        }
    }

    // Pickup logic
    public synchronized void pickupOrder(String orderId, List<Action> actions) {
        if (cooler.pickupOrder(orderId, actions) || heater.pickupOrder(orderId, actions) || shelf.pickupOrder(orderId, actions) ) {
            actions.add(new Action(Instant.now(), orderId, Action.PICKUP));
        }
    }

    private void logAction(String action, String orderId) {

        actionLog.add(String.format("%d: %s %s", System.currentTimeMillis(), action, orderId));
    }

    public List<String> getActionLog() {
        return actionLog;
    }

    public void postMove(List<Action> actions, Order order){
        boolean exists = actions.stream()
                .anyMatch(action -> order.getId().equals(action.getId()));
        if(!exists){
            order.setTimestamp(Instant.now());
            actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            return;
        }
        actions.add(new Action(Instant.now(), order.getId(), Action.MOVE));
    }

    public void handleHotOrders(Order order, List<Action> actions){
        LOGGER.info(order.getId()+" : order is hot");
        if(heater.addOrder(order)){
            order.setTimestamp(Instant.now());
            LOGGER.info(order.getId()+" is placed in Heater");
            actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            return;
        }
        if(shelf.moveOrder(order)){
            LOGGER.info(order.getId()+" heater is full so we place order in shelf");
            boolean exists = actions.stream()
                    .anyMatch(action -> order.getId().equals(action.getId()));
            if(!exists){
                order.setTimestamp(Instant.now());
                actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
                return;
            }
            actions.add(new Action(Instant.now(), order.getId(), Action.MOVE));
            return;
        }
        LOGGER.info(order.getId()+" Shelf is full so we check for space in cooler");
        if(cooler.isFull()){
            LOGGER.info(order.getId()+" Cooler is full so removing old order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                LOGGER.info(order.getId()+" Old order discarded as cooler is full");
                actions.add(new Action(Instant.now(), discardedOrder.getId(), Action.DISCARD));
            }
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" Cooler is full so removing old order from shelf and added new order");
                order.setTimestamp(Instant.now());
                actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(order.getId()+" Cooler is not full so we move cold order from shelf to cooler");
        Order coldOrder = shelf.getLeastFreshColdOrder();
        if(coldOrder == null){
            LOGGER.info(order.getId()+" Did not find cold order on shelf so discard least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                actions.add(new Action(Instant.now(), discardedOrder.getId(), Action.DISCARD));
                logAction("discard", discardedOrder.getId());
            }
            if (shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" Added new order to shelf after discrading old order from shelf as we didnt find cold order on shelf");
                order.setTimestamp(Instant.now());
                actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(order.getId()+" We found cold order on shelf");
        if (cooler.addOrder(coldOrder)){
            LOGGER.info(order.getId()+" Cold order added in cooler and removed from shelf");
            shelf.removeOrder(coldOrder.getId());
            actions.add(new Action(Instant.now(), coldOrder.getId(), Action.MOVE));
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" New order added to shelf");
                order.setTimestamp(Instant.now());
                actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            }
        }

    }

    public void handleColdOrders(Order order, List<Action> actions){
        if(cooler.addOrder(order)){
            order.setTimestamp(Instant.now());
            actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            return;
        }
        if(shelf.moveOrder(order)){
            LOGGER.info(order.getId()+" Cooler is full so we place order in shelf");
            boolean exists = actions.stream()
                    .anyMatch(action -> order.getId().equals(action.getId()));
            if(!exists){
                order.setTimestamp(Instant.now());
                actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
                return;
            }
            actions.add(new Action(Instant.now(), order.getId(), Action.MOVE));
            return;
        }
        LOGGER.info(order.getId()+" Shelf is full so we check for space in Heater");
        if (heater.isFull()){
            LOGGER.info(order.getId()+" Heater is full so removing old order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                LOGGER.info(order.getId()+" Old order discarded as Heater is full");
                actions.add(new Action(Instant.now(), discardedOrder.getId(), Action.DISCARD));
                logAction("discard", discardedOrder.getId());
            }
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" Heater is full so removing old order from shelf and added new order");
                order.setTimestamp(Instant.now());
                actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(order.getId()+" Heater is not full so we move Hot order from shelf to Heater");
        Order hotOrder = shelf.getLeastFreshHotOrder();
        if(hotOrder == null){
            LOGGER.info(order.getId()+" Did not find Hot order on shelf so discard least fresh order from shelf");
            Order discardedOrder = shelf.getOrderToDiscard();
            if (discardedOrder != null) {
                shelf.removeOrder(discardedOrder.getId());
                actions.add(new Action(Instant.now(), discardedOrder.getId(), Action.DISCARD));
                logAction("discard", discardedOrder.getId());
            }
            if (shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" Added new order to shelf after discrading old order from shelf as we didnt find cold order on shelf");
                order.setTimestamp(Instant.now());
                actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            }
            return;
        }
        LOGGER.info(order.getId()+" We found Hot order on shelf");
        if (heater.addOrder(hotOrder)){
            LOGGER.info(order.getId()+" Cold order added in cooler and removed from shelf");
            shelf.removeOrder(hotOrder.getId());
            actions.add(new Action(Instant.now(), hotOrder.getId(), Action.MOVE));
            if(shelf.moveOrder(order)){
                LOGGER.info(order.getId()+" New order added to shelf");
                order.setTimestamp(Instant.now());
                actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            }
        }
    }

    public void handleRoomOrders(Order order, List<Action> actions){
        if(shelf.addOrder(order)){
            order.setTimestamp(Instant.now());
            actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
            return;
        }
        if (!heater.isFull()){
            Order hotOrder = shelf.getLeastFreshHotOrder();
            if(hotOrder != null){
                if (heater.addOrder(hotOrder)){
                    shelf.removeOrder(hotOrder.getId());
                    //updateAction(actions, hotOrder.getId(), Action.MOVE);
                    actions.add(new Action(Instant.now(), hotOrder.getId(), Action.MOVE));
                    if(shelf.moveOrder(order)){
                        order.setTimestamp(Instant.now());
                        //updateAction(actions, order.getId(), Action.PLACE);
                        actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
                        return;
                    }
                }
            }
        } else if (!cooler.isFull()) {
            Order coldOrder = shelf.getLeastFreshColdOrder();
            if(coldOrder != null){
                if (cooler.addOrder(coldOrder)){
                    shelf.removeOrder(coldOrder.getId());
                    //updateAction(actions, coldOrder.getId(), Action.MOVE);
                    actions.add(new Action(Instant.now(), coldOrder.getId(), Action.MOVE));
                    if(shelf.moveOrder(order)){
                        order.setTimestamp(Instant.now());
                        //updateAction(actions, order.getId(), Action.PLACE);
                        actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
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
            actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));
        }
    }

}

