package se.sics.kompics.testkit;

import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;

abstract class TestHandler extends Handler {
  Proxy proxy;
  PortStructure portStruct;
  TestHandler(
          Proxy proxy,
          PortStructure portStruct, Class<? extends KompicsEvent> eventType) {
    setEventType(eventType);
    this.proxy = proxy;
    this.portStruct = portStruct;
  }
}
