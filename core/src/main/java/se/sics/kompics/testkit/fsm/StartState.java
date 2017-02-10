package se.sics.kompics.testkit.fsm;

import se.sics.kompics.ComponentCore;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;

class StartState extends State {

  private ComponentCore proxy;
  StartState(ComponentCore proxy) {
    this.proxy = proxy;
  }

  @Override
  protected boolean run() {
    Kompics.logger.info("Start State!");
    proxy.getControl().doTrigger(Start.event, 0, proxy);
    return true;
  }
}
