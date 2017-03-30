package se.sics.kompics.testkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.Request;
import se.sics.kompics.Response;

class InboundHandler extends ProxyHandler {

  private Port<? extends PortType> destPort;

  InboundHandler(
          Proxy proxy, PortStructure portStruct, Class<? extends KompicsEvent> eventType) {
    super(proxy, portStruct, eventType);
    this.destPort = portStruct.getOutboundPort();
  }

  @Override
  public void handle(KompicsEvent event) {
    Testkit.logger.trace("received incoming event: {}", event);
    if (event instanceof Response) {
      Response response = (Response) event;
      assert response.getTopPathElementFirst().getComponent() == destPort.getPair().getOwner();
    } else if (event instanceof Request) {
      Request request = (Request) event;
      request.pushPathElement(proxy.getComponentCore());
    }

    EventSpec<? extends KompicsEvent> eventSpec = proxy.getFsm().newEventSpec(event, destPort, Direction.INCOMING);
    eventSpec.setHandler(this);
    eventQueue.offer(eventSpec);
  }

  @Override
  public void doHandle(KompicsEvent event) {
    destPort.doTrigger(event, 0, proxy.getComponentCore());
  }
}
