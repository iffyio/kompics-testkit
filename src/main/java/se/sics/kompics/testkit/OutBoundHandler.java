package se.sics.kompics.testkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ChannelCore;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.Request;
import se.sics.kompics.RequestPathElement;
import se.sics.kompics.Response;

import java.util.List;

class OutBoundHandler extends ProxyHandler {

  private Port<? extends PortType> sourcePort;
  private List<? extends Port<? extends PortType>> destPorts;

  OutBoundHandler(
          Proxy proxy, PortStructure portStruct, Class<? extends KompicsEvent> eventType) {
    super(proxy, portStruct, eventType);
    this.sourcePort = portStruct.getOutboundPort();
    this.destPorts = portStruct.getConnectedPorts();
  }

  @Override
  public void handle(KompicsEvent event) {
    Testkit.logger.trace("received event: {}, connected to {} ports", event, destPorts.size());

    if (event instanceof Request) {
      Request request = (Request) event;
      request.pushPathElement(proxy.getComponentCore());
    }

    EventSpec<? extends KompicsEvent> eventSpec = proxy.getFsm().newEventSpec(event, sourcePort, Direction.OUTGOING);
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
      if (portStruct.isProvidedPort) {
        caller.forwardToNegative(response, 0);
      } else {
        caller.forwardToPositive(response, 0);
      }
    }
  }
}
