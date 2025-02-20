package com.css.challenge;

import com.css.challenge.client.Action;
import com.css.challenge.client.Client;
import com.css.challenge.client.Order;
import com.css.challenge.client.Problem;
import com.css.challenge.management.OrderManagementSystem;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

@Command(name = "challenge", showDefaultValues = true)
public class Main implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Properties properties = new Properties();

    static {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT: %5$s %n");
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Configuration file not found!");
            }
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

            long TOTAL_RUNTIME = Long.parseLong(get("total.runtime")); //Approx. time for application to finish processing all orders

            OrderManagementSystem oms = new OrderManagementSystem();
            List<Action> actions = new ArrayList<>();
            List<Order> orders = new ArrayList<>();

            long startTime = System.currentTimeMillis(); // Maintaining start time

            Thread orderPlacer = new Thread(()->{
                for(int i = 1; i <= problem.getOrders().size(); i++){
                    Order order = problem.getOrders().get(i-1);
                    LOGGER.info("Received: {}", order);
                    order.setTimestamp(Instant.now());
                    oms.placeOrder(order, actions);
                    synchronized (orders){
                        orders.add(order);
                    }
                    try {
                        Thread.sleep(rate.toMillis());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            Thread orderPickUp = new Thread(()->{
                while (System.currentTimeMillis() - startTime < TOTAL_RUNTIME){
                    try {
                        Thread.sleep(Long.parseLong(get("thread.pause"))); // Small delay to reduce CPU usage
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    synchronized (orders){
                        Iterator<Order> orderIterator = orders.iterator();
                        while (orderIterator.hasNext()){
                            Order order = orderIterator.next();
                            long currentTime = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
                            long elapsed = (currentTime - order.getTimestamp())/Long.parseLong(get("epoch.to.seconds")); //converting into Seconds
                            if(elapsed >= min.toSeconds() && elapsed <= max.toSeconds()){
                                oms.pickupOrder(order, actions);
                                orderIterator.remove();
                            }else if (elapsed > max.toMillis()) {
                                orderIterator.remove();
                            }
                        }
                    }
                }
            });

            orderPlacer.start();
            orderPickUp.start();

            orderPlacer.join();
            orderPickUp.join();

            LOGGER.info("Total time to all orders "+(System.currentTimeMillis() - startTime));

            String result = client.solveProblem(problem.getTestId(), rate, min, max, actions);
            LOGGER.info("Result: {}", result);

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Simulation failed: {}", e.getMessage());
        }


    }

    // Method to get a property value
    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}
