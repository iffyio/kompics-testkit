package se.sics.kompics.testkit.fsm;

import se.sics.kompics.Kompics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StateTable {

  private Map<Integer, List<Action>> table = new HashMap<>();


  void addStateClause(int stateIndex, EventSpec eventSpec, int nextState, Environment env) {
    List<Action> entry = getOrCreateStateEntry(stateIndex, env);
    entry.add(new Action(eventSpec, true, nextState));
  }

  Action lookUp(int stateIndex, EventSpec eventSpec) {
    List<Action> actions = table.get(stateIndex);
    assert actions != null;

    for (Action a : actions) {
      if (a.matches(eventSpec)) {
        return a;
      }
    }

    Kompics.logger.info("spec = {}", eventSpec);
    for (Action a : actions)
      Kompics.logger.info("{}", a);

    return new Action(null, false, -1);
  }

  void printTable(int final_state) {
    for (int i = 0; i <= final_state; i++) {
      List<Action> as = table.get(i);
      if (as != null) {
        System.out.println(i);
        for (Action a : as) {
          System.out.println("\t\t" + a);
        }
      }
    }
  }

  private List<Action> getOrCreateStateEntry(int stateIndex, Environment env) {
    List<Action> row = table.get(stateIndex);

    if (row != null) {
      return row;
    }

    row = new ArrayList<>();
    table.put(stateIndex, row);
    for (EventSpec e : env.getDisallowedEvents()) {
      row.add(new Action(e, false, FSM.ERROR_STATE));
    }

    for (EventSpec e : env.getAllowedEvents()) {
      row.add(new Action(e, true, stateIndex));
    }

    for (EventSpec e : env.getDroppedEvents()) {
      row.add(new Action(e, false, FSM.ERROR_STATE));
    }

    return row;
  }

  class Action {
    final EventSpec eventSpec;
    final boolean handle;
    final int nextIndex;
    Action(EventSpec eventSpec, boolean handle, int nextIndex) {
      this.eventSpec = eventSpec;
      this.handle = handle;
      this.nextIndex = nextIndex;
    }

    public boolean handleEvent() {
      return handle;
    }

    boolean matches(EventSpec eventSpec) {
      return this.eventSpec.equals(eventSpec);
    }

    public String toString() {
      return "( " + eventSpec + " ) " + (handle? "handle " : "drop ") + nextIndex;
    }
  }

}
