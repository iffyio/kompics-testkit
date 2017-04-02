package se.sics.kompics.testkit;

abstract class Spec {
  abstract StateTable.Transition getTransition(EventSpec receivedSpec, int state);
}
