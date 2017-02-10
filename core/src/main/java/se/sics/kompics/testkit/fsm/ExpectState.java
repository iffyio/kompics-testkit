package se.sics.kompics.testkit.fsm;

import se.sics.kompics.*;
import se.sics.kompics.testkit.EventSpec;
import se.sics.kompics.testkit.TestKit;

public class ExpectState extends State{

  private final FSM fsm;
  private final EventSpec expectedSpec;

  public ExpectState(
          FSM fsm, EventSpec expectedSpec) {

    this.fsm = fsm;
    this.expectedSpec = expectedSpec;
  }

  @Override
  protected boolean run() {
    Kompics.logger.info("expect: Expecting {}", expectedSpec);
    EventSpec queuedSpec = fsm.pollEventQueue();
    assert queuedSpec != null;

    if (expectedSpec.equals(queuedSpec)) {
      Kompics.logger.info("expect: PASS");
      queuedSpec.handle();
      return true;
    } else {
      Kompics.logger.info("expect: FAILED -> received {} instead", expectedSpec, queuedSpec);
      return false;
    }
  }

}
