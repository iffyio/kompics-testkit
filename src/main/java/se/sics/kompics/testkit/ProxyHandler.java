package se.sics.kompics.testkit;

import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;

abstract class ProxyHandler extends Handler {
  final Proxy proxy;
  final PortStructure portStruct;
  final EventQueue eventQueue;

  ProxyHandler(
          Proxy proxy, PortStructure portStruct, Class<? extends KompicsEvent> eventType) {
    setEventType(eventType);
    this.proxy = proxy;
    this.eventQueue = proxy.getEventQueue();
    this.portStruct = portStruct;
  }

  abstract void doHandle(KompicsEvent event);
}
