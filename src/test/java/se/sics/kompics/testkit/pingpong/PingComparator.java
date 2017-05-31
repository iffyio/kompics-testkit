package se.sics.kompics.testkit.pingpong;

import java.util.Comparator;

public class PingComparator implements Comparator<Ping> {

  @Override
  public int compare(Ping p1, Ping p2) {
    return p1.count - p2.count;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj instanceof PingComparator;
  }
}
