package se.sics.kompics.testkit.fsm;

import se.sics.kompics.ComponentCore;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;

class StartState extends State {

  private ComponentCore parent;
  StartState(ComponentCore parent) {
    this.parent = parent;
  }

  @Override
  protected boolean run() {
    Kompics.logger.info("Start State!");
    parent.getControl().doTrigger(Start.event, 0, parent);
    return true;
  }
}
