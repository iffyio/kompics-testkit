package se.sics.kompics.testkit;

import se.sics.kompics.*;

import java.util.*;

class PortStructure<P extends PortType> {
  private List<Port<P>> connectedPorts;
  private Port<P> ownerPort;
  private boolean isPositive;
  private Port<? extends PortType> inPort;
  private Port<? extends PortType> outPort;
  private Proxy proxy;
  private boolean isMockedPort;

  private Set<IncomingHandler> incomingHandlers;
  private Set<OutgoingHandler> outgoingHandlers;

  PortStructure(Proxy proxy, Port<P> ownerPort, boolean isPositive) {
    initialize(proxy, ownerPort);
    PortType portType = ownerPort.getPortType();
    this.isPositive = isPositive;
    inPort = isPositive? proxy.provideProxy(portType.getClass()):
            proxy.requireProxy(portType.getClass());
    outPort = inPort.getPair();
    isMockedPort = false;
  }

  PortStructure(Proxy proxy, Port<P> ownerPort) {
    initialize(proxy, ownerPort);
    isMockedPort = true;
  }

  private void initialize(Proxy proxy, Port<P> ownerPort) {
    this.proxy = proxy;
    connectedPorts = new ArrayList<>();
    this.ownerPort = ownerPort;
    incomingHandlers = new HashSet<>();
    outgoingHandlers = new HashSet<>();
  }

  void addConnectedPort(PortCore<P> other, ChannelFactory factory) {
    assert !isMockedPort;
    if (isPositive) {
      factory.connect((PortCore<P>) outPort, other);
    } else {
      factory.connect(other, (PortCore<P>) outPort);
    }
  }

  void addIncomingHandler(KompicsEvent event) {
    Class<? extends KompicsEvent> eventType = event.getClass();
    if (hasEquivalentHandler(eventType, TestKit.Direction.INCOMING)) {
      // already have a capable handler for this event
      Kompics.logger.info("ignoring unnecessary incoming handler");
      return;
    }

    IncomingHandler incomingHandler = new IncomingHandler(eventType, connectedPorts, ownerPort);
    incomingHandlers.add(incomingHandler);
    inPort.doSubscribe(incomingHandler);
  }

  void addOutgoingHandler(KompicsEvent event) {
    Class<? extends KompicsEvent> eventType = event.getClass();
    if (hasEquivalentHandler(eventType, TestKit.Direction.OUTGOING)) {
      // already have a capable handler for this event
      Kompics.logger.info("ignoring unnecessary outgoing handler");
      return;
    }

    OutgoingHandler outgoingHandler = new OutgoingHandler(eventType, ownerPort, connectedPorts);
    outgoingHandlers.add(outgoingHandler);
    ownerPort.doSubscribe(outgoingHandler);
  }

  private boolean hasEquivalentHandler(
          Class<? extends KompicsEvent> eventType, TestKit.Direction direction) {
    Collection<? extends Handler> handlers = direction == TestKit.Direction.INCOMING?
            incomingHandlers : outgoingHandlers;
    for (Handler h : handlers) {
      if (h.getEventType().isAssignableFrom(eventType)) {
        return true;
      }
    }

    return false;
  }

}
