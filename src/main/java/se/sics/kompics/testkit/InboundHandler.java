package se.sics.kompics.testkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.testkit.fsm.EventSpec;

class InboundHandler extends ProxyHandler {
  private static final Logger logger = LoggerFactory.getLogger(InboundHandler.class);

  private Port<? extends PortType> destPort;

  InboundHandler(Proxy proxy, PortStructure portStruct,
                 Class<? extends KompicsEvent> eventType) {
    super(proxy, portStruct, eventType);
    this.destPort = portStruct.getOutboundPort();
  }

  @Override
  public void handle(KompicsEvent event) {
    //logger.warn("received event: {}", event);
    if (event instanceof Response) {
      Response response = (Response) event;
      assert response.getTopPathElementFirst().getComponent() == destPort.getPair().getOwner();
    } else if (event instanceof Request) {
      Request request = (Request) event;
      request.pushPathElement(proxy.getComponentCore());
    }

    EventSpec eventSpec = new EventSpec<>(event, destPort, Direction.INCOMING);
    eventSpec.setHandler(this);
    eventQueue.offer(eventSpec);
  }

  @Override
  public void doHandle(KompicsEvent event) {
    destPort.doTrigger(event, 0, proxy.getComponentCore());
  }
}
