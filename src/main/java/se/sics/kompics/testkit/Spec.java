package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;

abstract class Spec {

  abstract StateTable.Transition getTransition(EventSpec<? extends KompicsEvent> receivedSpec, int state);
}
