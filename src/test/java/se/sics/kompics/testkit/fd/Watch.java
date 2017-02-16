package se.sics.kompics.testkit.fd;

import se.sics.kompics.KompicsEvent;

public class Watch implements KompicsEvent {

  public final TAddress node;

  public Watch(TAddress node) {
    this.node = node;
  }
}
