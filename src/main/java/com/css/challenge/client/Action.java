package com.css.challenge.client;

/** Action is a json-friendly representation of an action. */
public class Action {
  public static final String PLACE = "place";
  public static final String MOVE = "move";
  public static final String PICKUP = "pickup";
  public static final String DISCARD = "discard";

  private final long timestamp; // unix timestamp in microseconds
  private final String id; // order id
  private String action; // place, move, pickup or discard

  public Action(long timestamp, String id, String action) {
    this.timestamp = timestamp;
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
