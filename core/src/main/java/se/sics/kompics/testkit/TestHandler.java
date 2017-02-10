package se.sics.kompics.testkit;

import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.testkit.fsm.EventQueue;

abstract class TestHandler extends Handler {
  final Proxy proxy;
  final PortStructure portStruct;
  final EventQueue eventQueue;
  TestHandler(
          Proxy proxy,
          PortStructure portStruct, Class<? extends KompicsEvent> eventType) {
    setEventType(eventType);
    this.proxy = proxy;
    this.eventQueue = proxy.getEventQueue();
    this.portStruct = portStruct;
  }
}
