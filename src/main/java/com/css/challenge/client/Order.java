package com.css.challenge.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Order is a json-friendly representation of an order. */
public class Order {
  private final String id; // order id
  private final String name; // food name
  private final String temp; // ideal temperature
  private int freshness; // freshness in seconds
  private long timestamp; // Timestamp when order is placed

  public Order(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("temp") String temp,
      @JsonProperty("freshness") int freshness) {
    this.id = id;
    this.name = name;
    this.temp = temp;
    this.freshness = freshness;
  }

  static List<Order> parse(String json) throws JsonProcessingException {
    return new ObjectMapper().readValue(json, new TypeReference<List<Order>>() {});
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getTemp() {
    return temp;
  }

  public int getFreshness() {
    return freshness;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setFreshness(int freshness) {
    this.freshness = freshness;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = ChronoUnit.MICROS.between(Instant.EPOCH, timestamp);
  }

  @Override
  public String toString() {
    return "{id: " + id + ", name: " + name + ", temp: " + temp + ", freshness:" + freshness + " }";
  }
}
