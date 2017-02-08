package se.sics.kompics.testkit;

import se.sics.kompics.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class PortStructure<P extends PortType> {
  private Set<Port<P>> connectedPorts;
  private Port<P> ownerPort;
  private boolean isPositive;
  private Component proxyComponent;
  private Proxy proxy;
  private Port<? extends PortType> inPort;
  private Port<? extends PortType> outPort;
  private Parent parent;
  private boolean isMockedPort;

  private Set<IncomingHandler> incomingHandlers;
  private Set<OutgoingHandler> outgoingHandlers;

  PortStructure(Parent parent, Port<P> ownerPort, boolean isPositive) {
    initialize(parent, ownerPort);
    PortType portType = ownerPort.getPortType();
    this.isPositive = isPositive;
    proxyComponent = parent.createNewSetupComponent(Proxy.class, Init.NONE);
    proxy = (Proxy) proxyComponent.getComponent();
    inPort = isPositive? proxy.provideProxy(portType.getClass()):
            proxy.requireProxy(portType.getClass());
    outPort = inPort.getPair();
    isMockedPort = false;
  }

  PortStructure(Parent parent, Port<P> ownerPort) {
    initialize(parent, ownerPort);
    isMockedPort = true;
  }

  private void initialize(Parent parent, Port<P> ownerPort) {
    this.parent = parent;
    connectedPorts = new HashSet<>();
    this.ownerPort = ownerPort;
    incomingHandlers = new HashSet<>();
    outgoingHandlers = new HashSet<>();
  }

  void addConnectedPort(PortCore<P> other, ChannelFactory factory) {
    assert !isMockedPort;
    if (connectedPorts.add(other)) {
      if (isPositive) {
        factory.connect((PortCore<P>) outPort, other);
      } else {
        factory.connect(other, (PortCore<P>) outPort);
      }
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
      Kompics.logger.error("ignoring unnecessary outgoing handler");
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
