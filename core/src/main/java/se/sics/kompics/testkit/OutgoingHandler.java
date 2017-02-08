package se.sics.kompics.testkit;

import se.sics.kompics.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class OutgoingHandler extends TestHandler {
  private Port sourcePort;
  private Set<? extends Port<? extends PortType>> destPorts;

  OutgoingHandler(Class<? extends KompicsEvent> eventType,
          Port sourcePort, Collection<? extends Port<? extends PortType>> destPorts) {
    super(eventType);
    this.sourcePort = sourcePort;
    this.destPorts = new HashSet<>(destPorts);
  }

  @Override
  public void handle(KompicsEvent event) {
    Kompics.logger.error("OutgoingHandler");
  }
}
