package se.sics.kompics.testkit.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.testkit.EventSpec;

public class ExpectState extends State{
  private static final Logger logger = LoggerFactory.getLogger(ExpectState.class);
  private final EventSpec expectedSpec;

  public ExpectState(EventSpec expectedSpec) {
    this.expectedSpec = expectedSpec;
    this.requiresEvent = true;
  }

  @Override
  protected boolean runS() {
    logger.warn("Expecting {}", expectedSpec);
    while(true) {
      EventSpec queuedSpec = fsm.removeEventFromQueue();
      assert queuedSpec != null;

      if (getStateTable().isConditionallyDropped(queuedSpec)) {
        logger.warn("Dropped {}", queuedSpec);
      } else if (getStateTable().isWhitelisted(queuedSpec)) {
        queuedSpec.handle();
      }
      else if (expectedSpec.equals(queuedSpec)) {
        logger.warn("PASSED <=> {}", queuedSpec);
        queuedSpec.handle();
        return true;
      } else if (expectedSpec.getEvent().getClass().equals(ScheduleTimeout.class) && // temp shenanigans
              expectedSpec.getEvent().getClass().isAssignableFrom(queuedSpec.getEvent().getClass())) {
        logger.warn("CHECKED {} <=> {}", expectedSpec, queuedSpec);
        queuedSpec.handle();
        return true;
      } else {
        logger.warn("FAILED <=> expected {}, received {} instead", expectedSpec, queuedSpec);
        return false;
      }
    }
  }

}
