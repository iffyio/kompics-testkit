package se.sics.kompics.testkit.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.Start;

class StartState extends State {
  private static final Logger logger = LoggerFactory.getLogger(StartState.class);

  private ComponentCore proxy;
  StartState(ComponentCore proxy) {
    this.proxy = proxy;
  }

  @Override
  protected boolean runS() {
    logger.warn("Start State!");
    proxy.getControl().doTrigger(Start.event, 0, proxy);
    return true;
  }
}
