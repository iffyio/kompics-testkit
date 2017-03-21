package se.sics.kompics.testkit.fsm;

import se.sics.kompics.KompicsEvent;

interface Spec {
  boolean match(EventSpec<? extends KompicsEvent> receivedSpec);
}
