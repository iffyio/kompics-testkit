package se.sics.kompics.testkit;

import se.sics.kompics.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class OutgoingHandler extends TestHandler {
  private Port<? extends PortType> sourcePort;
  private List<? extends Port<? extends PortType>> destPorts;

  OutgoingHandler(
          Proxy proxy, PortStructure portStruct,
          Class<? extends KompicsEvent> eventType, Port sourcePort,
          Collection<? extends Port<? extends PortType>> destPorts) {
    super(proxy, portStruct, eventType);
    this.sourcePort = sourcePort;
    this.destPorts = new ArrayList<>(destPorts);
  }

  @Override
  public void handle(KompicsEvent event) {
    Kompics.logger.info("OutgoingHandler: {} received event: {}",
            this, event.getClass().getSimpleName());
    Kompics.logger.info("OutgoingHandler: connected ports: {}", destPorts.size());

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
