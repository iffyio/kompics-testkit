package se.sics.kompics.testkit;

import se.sics.kompics.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class IncomingHandler extends TestHandler {
  private List<Port<? extends PortType>> sourcePorts;
  private Port<? extends PortType> destPort;

  IncomingHandler(
          Proxy proxy, PortStructure portStruct,
          Class<? extends KompicsEvent> type, Collection<? extends Port<? extends PortType>> sourcePorts,
          Port<? extends PortType> destPort) {
    super(proxy, portStruct, type);
    this.destPort = destPort;
    this.sourcePorts = new ArrayList<>(sourcePorts);
  }

  @Override
  public void handle(KompicsEvent event) {
    Kompics.logger.info("IncomingHandler: {} received event: {}", this, event);
    if (event instanceof Response) {
      Response response = (Response) event;
      assert response.getTopPathElementFirst().getComponent() == destPort.getPair().getOwner();
    }
    EventSpec eventSpec = new EventSpec(event, destPort, TestKit.Direction.INCOMING);
    eventSpec.setHandler(this);
    eventQueue.offer(eventSpec);
  }

  @Override
  public void doHandle(KompicsEvent event) {
    destPort.doTrigger(event, 0, proxy.getComponentCore());
  }
}
