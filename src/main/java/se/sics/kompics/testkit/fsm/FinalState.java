package se.sics.kompics.testkit.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.testkit.EventSpec;

class FinalState extends State{
  private static final Logger logger = LoggerFactory.getLogger(FinalState.class);

  @Override
  protected boolean runS() {
    EventSpec unexpectedEvent = fsm.peekEventQueue();
    String msg = unexpectedEvent == null? "PASS" : "FAIL " + unexpectedEvent;
    logger.warn("Final State! = {}", msg);
    return true;
  }
}
