package se.sics.kompics.testkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;

import java.util.*;


class PortStructure {
  private static final Logger logger = LoggerFactory.getLogger(PortStructure.class);

  private List<Port<? extends PortType>> connectedPorts = new ArrayList<Port<? extends PortType>>();
  private Port<? extends PortType> inboundPort, outboundPort;

  private boolean isProvidedPort;
  private Proxy proxy;
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
        outBoundHandlers.add(outBoundHandler);
        outboundPort.doSubscribe(outBoundHandler);
      }
    }
  }

  private void subscribeInboundHandlersFor(Collection<Class<? extends KompicsEvent>> eventTypes) {
    for (Class<? extends KompicsEvent> eventType : eventTypes) {
      if (!hasEquivalentHandler(eventType, inboundHandlers)) {
        InboundHandler inboundHandler = new InboundHandler(proxy, this, eventType);
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

    PortCore<P> proxyOutsidePort = (PortCore<P>) inboundPort.getPair();

    if (isProvidedPort) {
      channel = factory.connect(proxyOutsidePort, connectedPort);
    } else {
      channel = factory.connect(connectedPort, proxyOutsidePort);
    }

    connectedPorts.add(connectedPort);
    portToChannel.put(connectedPort, channel);
  }

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
}
