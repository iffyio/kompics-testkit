package se.sics.kompics.testkit.fd;

import se.sics.kompics.KompicsEvent;

public class Restore implements KompicsEvent {

  public final TAddress node;

  public Restore(TAddress node) {
    this.node = node;
  }
  public boolean equals(Object o) {
    if (!(o instanceof Restore)) {
      return false;
    }

    Restore other = (Restore) o;
    return this.node.equals(other.node);
  }

  @Override
  public int hashCode() {
    return node.hashCode();
  }
}