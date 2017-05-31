package se.sics.kompics.testkit;

import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;

abstract class ProxyHandler extends Handler {
  Proxy proxy;
  PortStructure portStruct;
  EventQueue eventQueue;

  ProxyHandler(
          Proxy proxy, PortStructure portStruct, Class<? extends KompicsEvent> eventType) {
    setEventType(eventType);
    this.proxy = proxy;
    this.eventQueue = proxy.getEventQueue();
    this.portStruct = portStruct;
  }

  private ProxyHandler() { }

  abstract void doHandle(KompicsEvent event);

  static ProxyHandler faultHandler = new ProxyHandler() {
    @Override
    void doHandle(KompicsEvent event) { }

    @Override
    public void handle(KompicsEvent event) { }
  };
}
