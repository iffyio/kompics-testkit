package se.sics.kompics.testkit.fsm;

import se.sics.kompics.Kompics;

class FinalState extends State{

  @Override
  protected boolean run() {
    Kompics.logger.info("Final State!");
    return true;
  }
}
