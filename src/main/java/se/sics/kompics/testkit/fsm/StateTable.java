package se.sics.kompics.testkit.fsm;

import se.sics.kompics.KompicsEvent;

import java.util.HashMap;
import java.util.Map;

class StateTable {

  private Map<Integer, Map<EventSpec, Action>> table = new HashMap<>();
  private Map<Integer, EventSpec> eventSpecs = new HashMap<>();
  private Map<Integer, PredicateSpec> predicateSpecs = new HashMap<>();

  void registerExpectedEvent(int state, EventSpec eventSpec, Environment env) {
    int nextState = state + 1;
    Map<EventSpec, Action> entry = entryForState(state, env);
    Action action = new Action(eventSpec, Action.HANDLE, nextState);
    entry.put(eventSpec, action);
    eventSpecs.put(state, eventSpec);
  }

  void registerExpectedEvent(
          int state, PredicateSpec predSpec, Environment env) {
    createStateEntryIfNotExists(state, env);
    predicateSpecs.put(state, predSpec);
  }

  void printExpectedEventAt(int state) {
    FSM.logger.warn("{}: Expect\t{}", state,
            (predicateWasRegisteredForState(state)?
               predicateSpecs.get(state) : eventSpecs.get(state)));
  }

  Spec getExpectedSpecAt(int state) {
    return predicateWasRegisteredForState(state)?
             predicateSpecs.get(state) : eventSpecs.get(state);
  }

  Action lookup(int state, EventSpec receivedSpec) {
    Map<EventSpec, Action> entry = table.get(state);
    assert entry != null;

    if (predicateWasRegisteredForState(state)) {
      Action action = predicateLookup(state, receivedSpec);
      if (action != null) {
        return action;
      }
    }

    return entry.get(receivedSpec);
  }

  private boolean predicateWasRegisteredForState(int state) {
    return predicateSpecs.containsKey(state);
  }

  @SuppressWarnings("unchecked")
  private Action predicateLookup(int state, EventSpec eventSpec) {
    PredicateSpec predSpec = predicateSpecs.get(state);
    KompicsEvent receivedEvent = eventSpec.getEvent();

    if (predSpec != null && predSpec.getPredicate().apply(receivedEvent)) {
      int nextState = state + 1;
      return new Action(eventSpec, true, nextState);
    }

    return null;
  }

  void printTable(int final_state) {
    for (int i = 0; i <= final_state; i++) {
      Map<EventSpec, Action> entry = table.get(i);
      if (entry != null) {
        System.out.println(i);
        for (Action a : entry.values()) {
          System.out.println("\t\t" + a);
        }
        if (predicateWasRegisteredForState(i)) {
          System.out.printf("\t\t%s handle %d\n", predicateSpecs.get(i), i+1);
        }
      }
    }
  }

  private Map<EventSpec, Action> entryForState(int state, Environment env) {
    createStateEntryIfNotExists(state, env);
    return table.get(state);
  }

  private void createStateEntryIfNotExists(int state, Environment env) {
    Map<EventSpec, Action> entry = table.get(state);

    if (entry != null) {
      return;
    }

    entry = new HashMap<>();
    table.put(state, entry);
    for (EventSpec e : env.getDisallowedEvents()) {
      entry.put(e, new Action(e, Action.HANDLE, FSM.ERROR_STATE));
    }

    for (EventSpec e : env.getAllowedEvents()) {
      entry.put(e, new Action(e, Action.HANDLE, state));
    }

    for (EventSpec e : env.getDroppedEvents()) {
      entry.put(e, new Action(e, Action.DROP, state));
    }
  }

  static class Action {
    static boolean HANDLE = true;
    static boolean DROP = false;

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
