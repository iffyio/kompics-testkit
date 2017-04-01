package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

abstract class SingleEventSpec extends Spec{

  final Port<? extends PortType> port;
  final Direction direction;

  SingleEventSpec(Port<? extends PortType> port, Direction direction) {
    this.port = port;
    this.direction = direction;
  }

  abstract boolean match(EventSpec<? extends KompicsEvent> receivedSpec);

  @Override
  StateTable.Transition getTransition(EventSpec<? extends KompicsEvent> receivedSpec, int state) {
    return this.match(receivedSpec)? new StateTable.Transition(receivedSpec, state + 1) : null;
  }
}
