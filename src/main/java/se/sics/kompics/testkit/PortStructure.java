package se.sics.kompics.testkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;

import java.util.*;

class PortStructure<P extends PortType> {
  private static final Logger logger = LoggerFactory.getLogger(PortStructure.class);

  private List<Port<P>> connectedPorts;
  private Port<P> ownerPort;
  private boolean isPositive;
  private Port<? extends PortType> inPort;
  private Port<? extends PortType> outPort;
  private Proxy proxy;
  private boolean isMockedPort;
  private Map<Port, Channel> portToChannel = new HashMap<>();

  private Set<InboundHandler> inboundHandlers;
  private Set<OutBoundHandler> outBoundHandlers;

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
    inboundHandlers = new HashSet<>();
    outBoundHandlers = new HashSet<>();
  }

  void addConnectedPort(PortCore<P> other, ChannelFactory factory) {
    assert !isMockedPort;
    Channel<P> channel;
    if (isPositive) {
      channel = factory.connect((PortCore<P>) outPort, other);
    } else {
      channel = factory.connect(other, (PortCore<P>) outPort);
    }
    connectedPorts.add(other);
    portToChannel.put(other, channel);
  }

  void addIncomingHandler(KompicsEvent event) {
    Class<? extends KompicsEvent> eventType = event.getClass();
    if (hasEquivalentHandler(eventType, TestKit.Direction.INCOMING)) {
      // already have a capable handler for this event
/*      logger.warn("ignoring unnecessary incoming handler");*/
      return;
    }

    InboundHandler inboundHandler = new InboundHandler(proxy, this, eventType, connectedPorts, ownerPort);
    inboundHandlers.add(inboundHandler);
    inPort.doSubscribe(inboundHandler);
  }

  void addOutgoingHandler(KompicsEvent event) {
    Class<? extends KompicsEvent> eventType = event.getClass();
    if (hasEquivalentHandler(eventType, TestKit.Direction.OUTGOING)) {
      // already have a capable handler for this event
      logger.warn("ignoring unnecessary outgoing handler");
      return;
    }

    OutBoundHandler outBoundHandler = new OutBoundHandler(proxy, this, eventType, ownerPort, connectedPorts);
    outBoundHandlers.add(outBoundHandler);
    ownerPort.doSubscribe(outBoundHandler);
  }

  <P extends PortType> ChannelCore<P> getChannel(Port<P> port) {
    Channel channel = portToChannel.get(port);
    assert channel != null;
    return (ChannelCore<P>) channel;
  }

  private boolean hasEquivalentHandler(
          Class<? extends KompicsEvent> eventType, TestKit.Direction direction) {
    Collection<? extends Handler> handlers = direction == TestKit.Direction.INCOMING?
            inboundHandlers : outBoundHandlers;
    for (Handler h : handlers) {
      if (h.getEventType().isAssignableFrom(eventType)) {
        return true;
      }
    }

    return false;
  }

}
