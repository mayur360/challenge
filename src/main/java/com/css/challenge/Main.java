package com.css.challenge;

import com.css.challenge.client.Action;
import com.css.challenge.client.Client;
import com.css.challenge.client.Order;
import com.css.challenge.client.Problem;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import com.css.challenge.management.OrderManagementSystem;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "challenge", showDefaultValues = true)
public class Main implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static final Queue<Order> orderQueue = new LinkedBlockingQueue<>();

    static {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT: %5$s %n");
    }

    @Option(names = "--endpoint", description = "Problem server endpoint")
    String endpoint = "https://api.cloudkitchens.com";

    @Option(names = "--auth", description = "Authentication token (required)")
    String auth = "";

    @Option(names = "--name", description = "Problem name. Leave blank (optional)")
    String name = "";

    @Option(names = "--seed", description = "Problem seed (random if zero)")
    long seed = 0;

    @Option(names = "--rate", description = "Inverse order rate")
    Duration rate = Duration.ofMillis(500);

    @Option(names = "--min", description = "Minimum pickup time")
    Duration min = Duration.ofSeconds(4);

    @Option(names = "--max", description = "Maximum pickup time")
    Duration max = Duration.ofSeconds(8);

    @Override
    public void run() {
        try {
            Client client = new Client(endpoint, auth);
            Problem problem = client.newProblem(name, seed);

            // ------ Simulation harness logic goes here using rate, min and max ----

            ScheduledExecutorService pickupExecutor = Executors.newScheduledThreadPool(problem.getOrders().size());
            OrderManagementSystem oms = new OrderManagementSystem();
            Random random = new Random();
            List<Action> actions = new ArrayList<>();
            for (Order order : problem.getOrders()) {
                LOGGER.info("Received: {}", order);

                oms.placeOrder(order, actions);
                LOGGER.info("Order placed "+order.getId());
                Thread.sleep(rate.toMillis());
                // Schedule pickup after a random delay (4-8 seconds)
                pickupExecutor.schedule(()->oms.pickupOrder(order,actions), 500, TimeUnit.MILLISECONDS);
                /*new Thread(() -> {
                    Thread.currentThread().setName(order.getId());
                    *//*try {
                        Thread.sleep(random.nextInt(4500)); // 4-8 seconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*//*
                    oms.pickupOrder(order.getId(), actions, orderQueue);
                }).start();*/

                //actions.add(new Action(Instant.now(), order.getId(), Action.PLACE));

                pickupExecutor.execute(()->processUnpickedOrders(orderQueue, actions, oms));

            }
            pickupExecutor.shutdownNow(); // Interrupt pickup threads
            pickupExecutor.awaitTermination(10, TimeUnit.SECONDS);
            // ----------------------------------------------------------------------


            Thread.sleep(Duration.ofMillis(6000).toMillis());
            String result = client.solveProblem(problem.getTestId(), rate, min, max, actions);
            LOGGER.info("Result: {}", result);

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Simulation failed: {}", e.getMessage());
        }


    }

    private void processUnpickedOrders(Queue<Order> orderQueue, List<Action> actions, OrderManagementSystem oms){
        Iterator<Order> iterator = orderQueue.iterator();
        while(iterator.hasNext()){
            Order order = iterator.next();
            oms.pickupOrder(order, actions);
            iterator.remove();
        }
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}
