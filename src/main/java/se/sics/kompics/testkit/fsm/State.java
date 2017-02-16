package se.sics.kompics.testkit.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.testkit.EventSpec;

abstract class State {
  private static final Logger logger = LoggerFactory.getLogger(State.class);

  private StateTable stateTable;
  boolean requiresEvent = false;
  FSM fsm;

  void setStateTable(StateTable stateTable) {
    this.stateTable = stateTable;
  }

  StateTable getStateTable() {
    return stateTable;
  }

  void setFsm(FSM fsm) {
    this.fsm = fsm;
  }

  protected final boolean run() {
    EventSpec eventSpec;
    while (true) {
      eventSpec = fsm.peekEventQueue();
      if (eventSpec == null) {
        break; // empty event queue, run state
      } else if (stateTable.isBlacklisted(eventSpec)) {
        logger.warn("transition: received unexpected event {}", eventSpec);
        return false; // event // received unexpected event
      } else if (stateTable.isConditionallyDropped(eventSpec)) {
        logger.warn("transition: dropped event {}", eventSpec);
        fsm.removeEventFromQueue();
      } else if (stateTable.isWhitelisted(eventSpec)) {
        logger.warn("transition: allowed event {}", eventSpec);
        fsm.removeEventFromQueue();
        eventSpec.handle();
      } else {
        //logger.warn("transition: regular event {}", eventSpec);
        break; // regualar event may be handled
      }
    }
    return runS();
  }

  protected boolean runS() {
    return true;
  }
}
