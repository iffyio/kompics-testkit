package se.sics.kompics.testkit.pingpong;

import java.util.Comparator;

public class PongComparator implements Comparator<Pong> {

  @Override
  public int compare(Pong p1, Pong p2) {
    if (p1.count == p2.count) {
      return 0;
    } else {
      return -1;
    }
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj instanceof PongComparator;
  }
}
