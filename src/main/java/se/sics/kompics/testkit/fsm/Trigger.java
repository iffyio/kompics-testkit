package se.sics.kompics.testkit.fsm;

import se.sics.kompics.*;

public class Trigger extends State{

  private final KompicsEvent event;
  private final Port<? extends PortType> port;

  public Trigger(KompicsEvent event, Port<? extends PortType> port) {
    this.event = event;
    this.port = port;
  }

  @Override
  protected boolean runS() {
    // // TODO: 2/8/17 worker id, component?
    port.doTrigger(event, 0, port.getOwner());
    return true;
/*    Kompics.logger.info("trigger running");
    return true;*/
  }
}
