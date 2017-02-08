package se.sics.kompics.testkit.fsm;

import se.sics.kompics.ComponentCore;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

public class Trigger extends State{

  private final KompicsEvent event;
  private final Port<? extends PortType> port;

  public Trigger(KompicsEvent event, Port<? extends PortType> port) {
    this.event = event;
    this.port = port;
  }

  @Override
  protected boolean run() {
    // // TODO: 2/8/17 worker id, component?
    port.doTrigger(event, 0, port.getOwner());
    return true;
  }
}
