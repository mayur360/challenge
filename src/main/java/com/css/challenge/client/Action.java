package com.css.challenge.client;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Action is a json-friendly representation of an action. */
public class Action {
  public static final String PLACE = "place";
  public static final String MOVE = "move";
  public static final String PICKUP = "pickup";
  public static final String DISCARD = "discard";

  private final long timestamp; // unix timestamp in microseconds
  private final String id; // order id
  private String action; // place, move, pickup or discard

  public Action(Instant timestamp, String id, String action) {
    this.timestamp = ChronoUnit.MICROS.between(Instant.EPOCH, timestamp);
    this.id = id;
    this.action = action;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getId() {
    return id;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public String toString() {
    return "{timestamp: " + timestamp + ", id: " + id + ", action: " + action + " }";
  }
}
