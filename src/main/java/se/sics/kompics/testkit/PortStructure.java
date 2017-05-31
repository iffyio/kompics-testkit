package se.sics.kompics.testkit;

import se.sics.kompics.Channel;
import se.sics.kompics.ChannelCore;
import se.sics.kompics.ChannelFactory;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortCore;
import se.sics.kompics.PortType;
import se.sics.kompics.Unsafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


class PortStructure {

  private List<Port<? extends PortType>> connectedPorts = new ArrayList<Port<? extends PortType>>();
  private Port<? extends PortType> inboundPort, outboundPort;
  boolean isProvidedPort;

  private Proxy proxy;
  private Map<Port<? extends PortType>, Channel> portToChannel = new HashMap<Port<? extends PortType>, Channel>();

  private Set<InboundHandler> inboundHandlers = new HashSet<InboundHandler>();
  private Set<OutBoundHandler> outBoundHandlers = new HashSet<OutBoundHandler>();

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

  List<Port<? extends PortType>> getConnectedPorts() {
    return connectedPorts;
  }

  Port<? extends PortType> getOutboundPort() {
    return outboundPort;
  }

  <P extends PortType> void addConnectedPort(PortCore<P> connectedPort, ChannelFactory factory) {
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

  private void addProxyHandlers() {
    PortType portType = inboundPort.getPortType();
    Collection<Class<? extends KompicsEvent>> positiveEvents = Unsafe.getPositiveEvents(portType);
    Collection<Class<? extends KompicsEvent>> negativeEvents = Unsafe.getNegativeEvents(portType);

    if (isProvidedPort) {
      subscribeOutboundHandlersFor(positiveEvents);
      subscribeInboundHandlersFor(negativeEvents);
    } else {
      subscribeInboundHandlersFor(positiveEvents);
      subscribeOutboundHandlersFor(negativeEvents);
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
