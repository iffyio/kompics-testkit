package se.sics.kompics.testkit.fsm;

import se.sics.kompics.Kompics;

class FinalState extends State{

  @Override
  protected boolean runS() {
    Kompics.logger.info("Final State!{}",fsm.peekEventQueue());
    return true;
  }
}
