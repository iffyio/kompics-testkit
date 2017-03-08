package se.sics.kompics.testkit.fsm;

import com.google.common.base.Predicate;
import se.sics.kompics.KompicsEvent;

import java.util.*;

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

  private final Map<Integer, Map<EventSpec, Action>> table = new HashMap<Integer, Map<EventSpec, Action>>();

  private final Map<Integer, EventSpec> eventSpecs = new HashMap<Integer, EventSpec>();

  private final Map<Integer, PredicateSpec> predicateSpecs = new HashMap<Integer, PredicateSpec>();

  private final Map<Class<? extends KompicsEvent>, Predicate<? extends KompicsEvent>> defaultActions
          = new TreeMap<Class<? extends KompicsEvent>, Predicate<? extends KompicsEvent>>(eventComparator);

  private final TreeSet<Class<? extends KompicsEvent>> orderedTypes = new TreeSet<Class<? extends KompicsEvent>>();

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

  <E extends KompicsEvent> void setDefaultAction(Class<E> eventType,
                                                 Predicate<E> predicate) {
    defaultActions.put(eventType, predicate);
  }

  void printExpectedEventAt(int state) {
    FSM.logger.warn("{}: Expect\t{}", state, (predicateWasRegisteredFor(state)?
                                              predicateSpecs.get(state) : eventSpecs.get(state)));
  }

  Spec getExpectedSpecAt(int state) {
    return predicateWasRegisteredFor(state)?
             predicateSpecs.get(state) : eventSpecs.get(state);
  }

  Action lookup(int state, EventSpec receivedSpec) {
    Map<EventSpec, Action> entry = table.get(state);
    assert entry != null;

    Action action = null;

    if (predicateWasRegisteredFor(state)) {
      action = predicateLookup(state, receivedSpec);
    }

    if (action == null) {
      action = entry.get(receivedSpec);
    }

    if (action == null) {
      action = defaultLookup(state, receivedSpec);
    }

    return action;
  }

  private boolean predicateWasRegisteredFor(int state) {
    return predicateSpecs.containsKey(state);
  }

  //@SuppressWarnings("unchecked")
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
        if (predicateWasRegisteredFor(i)) {
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


  private Action defaultLookup(int state, EventSpec eventSpec) {

    Class<? extends KompicsEvent> eventType = eventSpec.getEvent().getClass();

    for (Class<? extends KompicsEvent> registeredType : defaultActions.keySet()) {
      if (registeredType.isAssignableFrom(eventType)) {

        if (eventShouldBeHandled(defaultActions.get(registeredType), eventSpec)) {
          return new Action(eventSpec, Action.HANDLE, state);
        } else {
          return new Action(eventSpec, Action.DROP, -1);
        }
      }
    }

    return null;
  }

  private <E extends KompicsEvent> boolean eventShouldBeHandled(Predicate<E> predicate,
                                                                EventSpec eventSpec) {
    E event = (E) eventSpec.getEvent();
    return predicate.apply(event);
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
