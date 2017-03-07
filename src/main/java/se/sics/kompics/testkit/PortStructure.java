package se.sics.kompics.testkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;

import java.util.*;

class PortStructure {
  private static final Logger logger = LoggerFactory.getLogger(PortStructure.class);

  private List<Port<? extends PortType>> connectedPorts = new ArrayList<Port<? extends PortType>>();
  private Port<? extends PortType> inboundPort, outboundPort;

  private Port<? extends PortType> ownerPort;
  private boolean isProvidedPort;
  private Port<? extends PortType> inPort;
  private Port<? extends PortType> outPort;
  private Proxy proxy;
  private boolean isMockedPort;
  private Map<Port, Channel> portToChannel = new HashMap<>();

  private Set<InboundHandler> inboundHandlers = new HashSet<>();
  private Set<OutBoundHandler> outBoundHandlers = new HashSet<>();

  PortStructure(Proxy proxy, Port<? extends PortType> inboundPort,
                Port<? extends PortType> outboundPort,
                boolean isProvidedPort) {

    this.proxy = proxy;
    this.inboundPort = inboundPort;
    this.outboundPort = outboundPort;
    this.isProvidedPort = isProvidedPort;

    assert inboundPort.getPortType() == outboundPort.getPortType();

    addProxyHandlers();
  }

  private void addProxyHandlers() {
    PortType portType = inboundPort.getPortType();
    if (isProvidedPort) {
      subscribeOutboundHandlersFor(portType.getPositiveEvents());
      subscribeInboundHandlersFor(portType.getNegativeEvents());
    } else {
      subscribeInboundHandlersFor(portType.getPositiveEvents());
      subscribeOutboundHandlersFor(portType.getNegativeEvents());
    }
  }

  private void subscribeOutboundHandlersFor(Collection<Class<? extends KompicsEvent>> eventTypes) {
    for (Class<? extends KompicsEvent> eventType : eventTypes) {
      if (!hasEquivalentHandler(eventType, outBoundHandlers)) {
        OutBoundHandler outBoundHandler = new OutBoundHandler(proxy, this, eventType);
        Kompics.logger.warn("subscribing {} ", eventType);
        outBoundHandlers.add(outBoundHandler);
        outboundPort.doSubscribe(outBoundHandler);
      }
    }
  }

  private void subscribeInboundHandlersFor(Collection<Class<? extends KompicsEvent>> eventTypes) {
    for (Class<? extends KompicsEvent> eventType : eventTypes) {
      if (!hasEquivalentHandler(eventType, inboundHandlers)) {
        InboundHandler inboundHandler = new InboundHandler(proxy, this, eventType);
        Kompics.logger.warn("subscribing {} ", eventType);
        inboundHandlers.add(inboundHandler);
        inboundPort.doSubscribe(inboundHandler);
      }
    }
  }

  List<Port<? extends PortType>> getConnectedPorts() {
    return connectedPorts;
  }

  Port<? extends PortType> getOutboundPort() {
    return outboundPort;
  }

  <P extends PortType> void addConnectedPort(PortCore<P> connectedPort,
                        ChannelFactory factory) {
    Channel<P> channel;

    if (isProvidedPort) {
      channel = factory.connect((PortCore<P>) outboundPort, connectedPort);
    } else {
      channel = factory.connect(connectedPort, (PortCore<P>) outboundPort);
    }

    connectedPorts.add(connectedPort);
    portToChannel.put(connectedPort, channel);
  }

/*  PortStructure(Proxy proxy, Port<P> ownerPort, boolean isProvidedPort) {
    initialize(proxy, ownerPort);
    PortType portType = ownerPort.getPortType();
    this.isProvidedPort = isProvidedPort;
    inPort = isProvidedPort? proxy.providePort(portType.getClass()):
            proxy.requirePort(portType.getClass());
    outPort = inPort.getPair();
    isMockedPort = false;
  }

  PortStructure(Proxy proxy, Port<P> ownerPort) {
    initialize(proxy, ownerPort);
    isMockedPort = true;
  }

  private void initialize(Proxy proxy, Port<P> ownerPort) {
    this.proxy = proxy;
    this.ownerPort = ownerPort;
    inboundHandlers = new HashSet<>();
    outBoundHandlers = new HashSet<>();
  }*/

/*  void addConnectedPort(PortCore<? extends PortType> other, ChannelFactory factory) {
    assert !isMockedPort;
    Channel<P> channel;
    if (isProvidedPort) {
      channel = factory.connect((PortCore<P>) outPort, other);
    } else {
      channel = factory.connect(other, (PortCore<P>) outPort);
    }
    connectedPorts.add(other);
    portToChannel.put(other, channel);
  }*/

/*  void addIncomingHandler(Class<? extends KompicsEvent> eventType) {
    if (hasEquivalentHandler(eventType, Direction.INCOMING)) {
      // already have a capable handler for this event
*//*      logger.warn("ignoring unnecessary incoming handler");*//*
      return;
    }

    InboundHandler inboundHandler = new InboundHandler(proxy, this, eventType, connectedPorts, ownerPort);
    inboundHandlers.add(inboundHandler);
    inPort.doSubscribe(inboundHandler);
  }

  void addOutgoingHandler(Class<? extends KompicsEvent> eventType) {
    if (hasEquivalentHandler(eventType, Direction.OUTGOING)) {
      // already have a capable handler for this event
*//*      logger.warn("ignoring unnecessary outgoing handler");*//*
      return;
    }

    OutBoundHandler outBoundHandler = new OutBoundHandler(proxy, this, eventType, ownerPort, connectedPorts);
    outBoundHandlers.add(outBoundHandler);
    ownerPort.doSubscribe(outBoundHandler);
  }*/

  <P extends PortType> ChannelCore<P> getChannel(Port<P> port) {
    Channel channel = portToChannel.get(port);
    assert channel != null;
    return (ChannelCore<P>) channel;
  }

  private boolean hasEquivalentHandler(
          Class<? extends KompicsEvent> eventType, Collection<? extends Handler> handlers) {
    for (Handler h : handlers) {
      if (h.getEventType().isAssignableFrom(eventType)) {
        return true;
      }
    }

    return false;
  }

  boolean isMockedPort() {
    return isMockedPort;
  }

}
