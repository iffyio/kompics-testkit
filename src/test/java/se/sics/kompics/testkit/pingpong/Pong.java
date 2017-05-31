package se.sics.kompics.testkit.pingpong;

import se.sics.kompics.KompicsEvent;

public class Pong implements KompicsEvent {
  public int count = 0;

  public Pong(int count) {
    this.count = count;
  }

  public String toString() {
    return "Pong " + count;
  }
}
