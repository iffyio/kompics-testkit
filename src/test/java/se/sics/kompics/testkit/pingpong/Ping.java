package se.sics.kompics.testkit.pingpong;

import se.sics.kompics.KompicsEvent;

public class Ping implements KompicsEvent {
  public int count = 0;

  public Ping(int count) {
    this.count = count;
  }

  public String toString() {
    return "Ping " + count;
  }
}
