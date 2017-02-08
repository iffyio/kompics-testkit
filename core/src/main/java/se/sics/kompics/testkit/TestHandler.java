package se.sics.kompics.testkit;

import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;

abstract class TestHandler extends Handler {
  TestHandler(Class<? extends KompicsEvent> eventType) {
    setEventType(eventType);
  }
}
