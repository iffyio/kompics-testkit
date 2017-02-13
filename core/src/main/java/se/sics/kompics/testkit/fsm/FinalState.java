package se.sics.kompics.testkit.fsm;

import se.sics.kompics.Kompics;
import se.sics.kompics.testkit.EventSpec;

class FinalState extends State{

  @Override
  protected boolean runS() {
    EventSpec unexpectedEvent = fsm.peekEventQueue();
    String msg = unexpectedEvent == null? "PASS" : "FAIL " + unexpectedEvent;
    Kompics.logger.info("Final State! = {}", msg);
    return true;
  }
}
