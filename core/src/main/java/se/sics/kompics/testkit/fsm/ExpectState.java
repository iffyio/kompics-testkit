package se.sics.kompics.testkit.fsm;

import se.sics.kompics.*;
import se.sics.kompics.testkit.EventSpec;

public class ExpectState extends State{

  private final EventSpec expectedSpec;

  public ExpectState(EventSpec expectedSpec) {
    this.expectedSpec = expectedSpec;
    this.requiresEvent = true;
  }

  @Override
  protected boolean runS() {
    Kompics.logger.info("expect: Expecting {}", expectedSpec);
    EventSpec queuedSpec = fsm.removeEventFromQueue();
    assert queuedSpec != null;

    if (expectedSpec.equals(queuedSpec)) {
      Kompics.logger.info("expect: PASS");
      queuedSpec.handle();
      return true;
    } else {
      Kompics.logger.info("expect: FAILED -> received {} instead", expectedSpec, queuedSpec);
      return false;
    }
/*    Kompics.logger.info("expect running");
    return true;*/
  }

}
