package se.sics.kompics.testkit.fsm;

import com.google.common.base.Function;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.testkit.Action;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


class StateTable {

  private final Comparator<Class<? extends KompicsEvent>> eventComparator = new Comparator<Class<? extends KompicsEvent>>() {
    @Override
    public int compare(Class<? extends KompicsEvent> e1, Class<? extends KompicsEvent> e2) {
      if (e1 == e2) {
        return 0;
      } else if (e1.isAssignableFrom(e2)) {
        return 1;
      } else {
        return -1;
      }
    }
  };

  private final Map<Integer, Map<EventSpec, Transition>> table = new HashMap<Integer, Map<EventSpec, Transition>>();

  private final Map<Integer, EventSpec> eventSpecs = new HashMap<Integer, EventSpec>();

  private final Map<Integer, PredicateSpec> predicateSpecs = new HashMap<Integer, PredicateSpec>();

  private final Map<Class<? extends KompicsEvent>, Function<? extends KompicsEvent, Action>> defaultActions =
          new TreeMap<Class<? extends KompicsEvent>, Function<? extends KompicsEvent, Action>>(eventComparator);

  void registerExpectedEvent(int state, EventSpec eventSpec, Environment env) {
    int nextState = state + 1;
    Map<EventSpec, Transition> entry = entryForState(state, env);
    Transition transition = new Transition(eventSpec, Action.HANDLE, nextState);
    entry.put(eventSpec, transition);
    eventSpecs.put(state, eventSpec);
  }

  void registerExpectedEvent(
          int state, PredicateSpec predSpec, Environment env) {
    createStateEntryIfNotExists(state, env);
    predicateSpecs.put(state, predSpec);
  }

  <E extends KompicsEvent> void setDefaultAction(
          Class<E> eventType, Function<E, Action> predicate) {
    defaultActions.put(eventType, predicate);
  }

  void printExpectedEventAt(int state) {
    FSM.logger.warn("{}: Expect\t{}", state, getExpectedSpecAt(state));
  }

  Spec getExpectedSpecAt(int state) {
    return predicateWasRegisteredFor(state)? predicateSpecs.get(state) : eventSpecs.get(state);
  }

  Transition lookup(int state, EventSpec receivedSpec) {
    Map<EventSpec, Transition> entry = table.get(state);
    assert entry != null;

    Transition transition = null;

    if (predicateWasRegisteredFor(state)) {
      transition = predicateLookup(state, receivedSpec);
    }

    if (transition == null) {
      transition = entry.get(receivedSpec);
    }

    if (transition == null) {
      transition = defaultLookup(state, receivedSpec);
    }

    return transition;
  }

  private boolean predicateWasRegisteredFor(int state) {
    return predicateSpecs.containsKey(state);
  }

  //@SuppressWarnings("unchecked")
  private Transition predicateLookup(int state, EventSpec eventSpec) {
    PredicateSpec predSpec = predicateSpecs.get(state);
    KompicsEvent receivedEvent = eventSpec.getEvent();

    if (predSpec != null && predSpec.getPredicate().apply(receivedEvent)) {
      int nextState = state + 1;
      return new Transition(eventSpec, Action.HANDLE, nextState);
    }

    return null;
  }

  void printTable(int final_state) {
    for (int i = 0; i <= final_state; i++) {
      Map<EventSpec, Transition> entry = table.get(i);
      if (entry != null) {
        System.out.println(i);
        for (Transition a : entry.values()) {
          System.out.println("\t\t" + a);
        }
        if (predicateWasRegisteredFor(i)) {
          System.out.printf("\t\t%s action %d\n", predicateSpecs.get(i), i+1);
        }
      }
    }
  }

  private Map<EventSpec, Transition> entryForState(int state, Environment env) {
    createStateEntryIfNotExists(state, env);
    return table.get(state);
  }

  private void createStateEntryIfNotExists(int state, Environment env) {
    Map<EventSpec, Transition> entry = table.get(state);

    if (entry != null) {
      return;
    }

    entry = new HashMap<>();
    table.put(state, entry);
    for (EventSpec e : env.getDisallowedEvents()) {
      entry.put(e, new Transition(e, Action.FAIL, FSM.ERROR_STATE));
    }

    for (EventSpec e : env.getAllowedEvents()) {
      entry.put(e, new Transition(e, Action.HANDLE, state));
    }

    for (EventSpec e : env.getDroppedEvents()) {
      entry.put(e, new Transition(e, Action.DROP, state));
    }
  }

  private Transition defaultLookup(int state, EventSpec eventSpec) {

    KompicsEvent event = eventSpec.getEvent();
    Class<? extends KompicsEvent> eventType = event.getClass();

    for (Class<? extends KompicsEvent> registeredType : defaultActions.keySet()) {
      if (registeredType.isAssignableFrom(eventType)) {

        Action action = actionFor(event, defaultActions.get(registeredType));

        switch (action) {
          case HANDLE:
            return new Transition(eventSpec, Action.HANDLE, state);
          case DROP:
            return new Transition(eventSpec, Action.DROP, state);
          default:
            return new Transition(eventSpec, Action.FAIL, FSM.ERROR_STATE);
        }
      }
    }

    return null;
  }

  private <E extends KompicsEvent> Action actionFor(KompicsEvent event, Function<E, Action> function) {
    E e = (E) event;
    return function.apply(e);
  }


  static class Transition {

    final EventSpec eventSpec;
    final Action action;
    final int nextState;

    Transition(EventSpec eventSpec, Action action, int nextState) {
      this.eventSpec = eventSpec;
      this.action = action;
      this.nextState = nextState;
    }

    boolean handleEvent() {
      return action == Action.HANDLE;
    }

    // transitions are equal if they are for the same event
    public boolean equals(Object obj) {
      if (!(obj instanceof Transition)) {
        return false;
      }

      Transition other = (Transition) obj;
      return this.eventSpec.equals(other.eventSpec);
    }

    public int hashCode() {
      return eventSpec.hashCode();
    }

    public String toString() {
      return "( " + eventSpec + " ) " + (handleEvent()? "handle " : "drop ") + nextState;
    }
  }

}
