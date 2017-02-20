package se.sics.kompics.testkit.fsm;

import se.sics.kompics.Kompics;

import java.util.*;

class StateTable {

  private Map<Integer, Map<EventSpec, Action>> table = new HashMap<>();


  void addStateClause(int stateIndex, EventSpec eventSpec, int nextState, Environment env) {
    Map<EventSpec, Action> entry = getOrCreateStateEntry(stateIndex, env);
    Action action = new Action(eventSpec, true, nextState);
    entry.put(eventSpec, action);
  }

  // lookup action quicker
  Action lookUp(int stateIndex, EventSpec eventSpec) {
    Map<EventSpec, Action> entry = table.get(stateIndex);
    assert entry != null;


    return entry.get(eventSpec);
  }

  void printTable(int final_state) {
    for (int i = 0; i <= final_state; i++) {
      Map<EventSpec, Action> entry = table.get(i);
      if (entry != null) {
        System.out.println(i);
        for (Action a : entry.values()) {
          System.out.println("\t\t" + a);
        }
      }
    }
  }

  private Map<EventSpec, Action> getOrCreateStateEntry(int stateIndex, Environment env) {
    Map<EventSpec, Action> row = table.get(stateIndex);

    if (row != null) {
      return row;
    }

    row = new HashMap<>();
    table.put(stateIndex, row);
    for (EventSpec e : env.getDisallowedEvents()) {
      row.put(e, new Action(e, false, FSM.ERROR_STATE));
    }

    for (EventSpec e : env.getAllowedEvents()) {
      row.put(e, new Action(e, true, stateIndex));
    }

    for (EventSpec e : env.getDroppedEvents()) {
      row.put(e, new Action(e, false, FSM.ERROR_STATE));
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

    boolean handleEvent() {
      return handle;
    }

    boolean matches(EventSpec eventSpec) {
      return this.eventSpec.equals(eventSpec);
    }

    // actions are equal if they are for the same event
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof Action)) {
        return false;
      }

      Action other = (Action) obj;
      return this.eventSpec.equals(other.eventSpec);
    }

    public int hashCode() {
      return 31 * eventSpec.hashCode();
    }

    public String toString() {
      return "( " + eventSpec + " ) " + (handle? "handle " : "drop ") + nextIndex;
    }
  }

}
