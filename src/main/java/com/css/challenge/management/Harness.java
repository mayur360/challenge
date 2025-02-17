package com.css.challenge.management;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Harness {
    /*public static void main(String[] args) {
        List<Order> orders = List.of(
                new Order("e6bc2", "Cheese Pizza", "hot", 120),
                new Order("a1b2c", "Caesar Salad", "cold", 90),
                new Order("x9y8z", "Garlic Bread", "room", 60)
        );

        OrderManagementSystem oms = new OrderManagementSystem();
        Random random = new Random();

        // Place orders at a rate of 2 orders/second
        for (Order order : orders) {
            oms.placeOrder(order, new ArrayList<Action>());
            try {
                Thread.sleep(500); // 2 orders/second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Schedule pickup after a random delay (4-8 seconds)
            new Thread(() -> {
                try {
                    Thread.sleep(random.nextInt(4000) + 4000); // 4-8 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                oms.pickupOrder(order.getId());
            }).start();
        }

        // Wait for all pickups to complete
        try {
            Thread.sleep(10000); // Wait for 10 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Print action log
        oms.getActionLog().forEach(System.out::println);
    }*/
}
