package se.sics.kompics.testkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class OutBoundHandler extends ProxyHandler {
  private static final Logger logger = LoggerFactory.getLogger(OutBoundHandler.class);
  
  private Port<? extends PortType> sourcePort;
  private List<? extends Port<? extends PortType>> destPorts;

  OutBoundHandler(
          Proxy proxy, PortStructure portStruct,
          Class<? extends KompicsEvent> eventType, Port sourcePort,
          Collection<? extends Port<? extends PortType>> destPorts) {
    super(proxy, portStruct, eventType);
    this.sourcePort = sourcePort;
    this.destPorts = new ArrayList<>(destPorts);
  }

  @Override
  public void handle(KompicsEvent event) {
    //logger.warn("received event: {}", event.getClass().getSimpleName());
    //logger.warn("connected ports: {}", destPorts.size());

    if (event instanceof Request) {
      Request request = (Request) event;
      request.pushPathElement(proxy.getComponentCore());
    }

    EventSpec eventSpec = new EventSpec(event, sourcePort, TestKit.Direction.OUTGOING);
    eventSpec.setHandler(this);
    eventQueue.offer(eventSpec);
  }

  @Override
  public void doHandle(KompicsEvent event) {
    for (Port<? extends PortType> port : destPorts) {
      port.doTrigger(event, 0, portStruct.getChannel(port));
    }
  }
}
