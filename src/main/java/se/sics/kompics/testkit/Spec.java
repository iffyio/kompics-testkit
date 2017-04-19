package se.sics.kompics.testkit;

interface Spec {
  boolean match(EventSpec receivedSpec);
}
