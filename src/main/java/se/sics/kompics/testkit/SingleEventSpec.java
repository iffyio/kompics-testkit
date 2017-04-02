package se.sics.kompics.testkit;

import se.sics.kompics.Port;
import se.sics.kompics.PortType;

abstract class SingleEventSpec extends Spec{

  final Port<? extends PortType> port;
  final Direction direction;

  SingleEventSpec(Port<? extends PortType> port, Direction direction) {
    this.port = port;
    this.direction = direction;
  }

  abstract boolean match(EventSpec receivedSpec);

  @Override
  StateTable.Transition getTransition(EventSpec receivedSpec, int state) {
    return this.match(receivedSpec)? new StateTable.Transition(receivedSpec, Action.HANDLE, state + 1) : null;
  }
}
