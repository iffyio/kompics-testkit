package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

abstract class Spec {
  final Port<? extends PortType> port;
  final Direction direction;

  Spec(Port<? extends PortType> port, Direction direction) {
    this.port = port;
    this.direction = direction;
  }

  abstract boolean match(EventSpec<? extends KompicsEvent> receivedSpec);
}
