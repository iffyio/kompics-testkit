package se.sics.kompics.testkit.urb;

import se.sics.kompics.KompicsEvent;

public class UrbBroadcast implements KompicsEvent {

  final Counter counter;

  public UrbBroadcast(Counter counter) {
    this.counter = counter;
  }
}
