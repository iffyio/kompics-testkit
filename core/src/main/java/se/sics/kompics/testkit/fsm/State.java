package se.sics.kompics.testkit.fsm;

import se.sics.kompics.Kompics;
import se.sics.kompics.testkit.EventSpec;

abstract class State {

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
        Kompics.logger.info("transition: received unexpected event {}", eventSpec);
        return false; // event // received unexpected event
      } else if (stateTable.isConditionallyDropped(eventSpec)) {
        Kompics.logger.info("transition: dropped event {}", eventSpec);
        fsm.removeEventFromQueue();
      } else if (stateTable.isWhitelisted(eventSpec)) {
        Kompics.logger.info("transition: allowed event {}", eventSpec);
        fsm.removeEventFromQueue();
        eventSpec.handle();
      } else {
        Kompics.logger.info("transition: regular event {}", eventSpec);
        break; // regualar event may be handled
      }
    }
    return runS();
  }

  protected boolean runS() {
    return true;
  }
}
