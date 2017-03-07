package se.sics.kompics.testkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.testkit.fsm.EventSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class OutBoundHandler extends ProxyHandler {
  private static final Logger logger = LoggerFactory.getLogger(OutBoundHandler.class);
  
  private Port<? extends PortType> sourcePort;
  private List<? extends Port<? extends PortType>> destPorts;

  OutBoundHandler(
          Proxy proxy, PortStructure portStruct,
          Class<? extends KompicsEvent> eventType, Port<? extends PortType> sourcePort,
          Collection<? extends Port<? extends PortType>> destPorts) {
    super(proxy, portStruct, eventType);
    this.sourcePort = sourcePort;
    this.destPorts = new ArrayList<>(destPorts);
  }

  @Override
  public void handle(KompicsEvent event) {
/*    logger.warn("received event: {}", event);
    logger.warn("connected ports: {}", destPorts.size());*/

    if (event instanceof Request) {
      Request request = (Request) event;
      request.pushPathElement(proxy.getComponentCore());
    }

    EventSpec eventSpec = new EventSpec(event, sourcePort, Direction.OUTGOING);
    eventSpec.setHandler(this);
    eventQueue.offer(eventSpec);
  }

  @Override
  public void doHandle(KompicsEvent event) {
    if (event instanceof Response) {
      deliverToSingleChannel((Response) event);
    } else {
      deliverToAllConnectedPorts(event);
    }
  }

  private void deliverToAllConnectedPorts(KompicsEvent event) {
    for (Port<? extends PortType> port : destPorts) {
      port.doTrigger(event, 0, portStruct.getChannel(port));
    }
  }

  private void deliverToSingleChannel(Response response) {
    RequestPathElement pe = response.getTopPathElement();
    if (pe != null && pe.isChannel()) {
      ChannelCore<?> caller = pe.getChannel();
      // // TODO: 2/21/17 isPositivePort does not belong in Kompics core
      if (((PortCore)sourcePort).isPositivePort()) {
        caller.forwardToNegative(response, 0);
      } else {
        caller.forwardToPositive(response, 0);
      }
    }
  }
}
