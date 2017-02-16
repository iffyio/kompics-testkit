package se.sics.kompics.testkit.fd;

import se.sics.kompics.KompicsEvent;

public class Suspect implements KompicsEvent {

  public final TAddress node;

  public Suspect(TAddress node) {
    this.node = node;
  }

  public boolean equals(Object o) {
    if (!(o instanceof Suspect)) {
      return false;
    }

    Suspect other = (Suspect) o;
    return this.node.equals(other.node);
  }

  @Override
  public int hashCode() {
    return node.hashCode();
  }
}