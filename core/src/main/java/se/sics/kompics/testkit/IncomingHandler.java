package se.sics.kompics.testkit;

import se.sics.kompics.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class IncomingHandler extends TestHandler {
  Set<Port<? extends PortType>> sourcePorts;
  Port destPort;

  IncomingHandler(Class<? extends KompicsEvent> type,
                  Collection<? extends Port<? extends PortType>> sourcePorts, Port destPort) {
    super(type);
    this.destPort = destPort;
    this.sourcePorts = new HashSet<>(sourcePorts);
  }

  @Override
  public void handle(KompicsEvent event) {
    Kompics.logger.error("IncomingHandler");
  }
}
