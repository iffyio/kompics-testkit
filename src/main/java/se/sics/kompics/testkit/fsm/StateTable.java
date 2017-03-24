package se.sics.kompics.testkit.fsm;

import com.google.common.base.Function;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.testkit.Action;
import se.sics.kompics.testkit.Testkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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

  private final Map<Class<? extends KompicsEvent>, Function<? extends KompicsEvent, Action>> defaultActions =
          new TreeMap<Class<? extends KompicsEvent>, Function<? extends KompicsEvent, Action>>(eventComparator);

  private Map<Integer, State> states = new HashMap<Integer, State>();

  void registerExpectedEvent(int state, Spec spec, Block block) {
    states.put(state, new State(state, spec, block));
  }

  void registerExpectedEvent(int state, List<Spec> expectUnordered, Block block) {
    states.put(state, new State(state, expectUnordered, block));
  }

  <E extends KompicsEvent> void setDefaultAction(
          Class<E> eventType, Function<E, Action> predicate) {
    defaultActions.put(eventType, predicate);
  }

  void printExpectedEventAt(int state) {
    FSM.logger.debug("{}: Expect\t{}", state, getExpectedSpecAt(state));
  }

  String getExpectedSpecAt(int state) {
    assert states.get(state) != null;
    return states.get(state).toString();
  }

  boolean isExpectState(int state) {
    return states.containsKey(state);
  }

  Transition lookup(int state, EventSpec<? extends KompicsEvent> receivedSpec) {
    return states.get(state).onEvent(receivedSpec);
  }

  private Transition defaultLookup(int state, EventSpec eventSpec) {
    KompicsEvent event = eventSpec.getEvent();
    Class<? extends KompicsEvent> eventType = event.getClass();

    for (Class<? extends KompicsEvent> registeredType : defaultActions.keySet()) {
      if (registeredType.isAssignableFrom(eventType)) {
        Action action = actionFor(event, registeredType);
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

  StateTable.Transition lookupWithBlock(int state, EventSpec<? extends KompicsEvent> receivedSpec, Block block) {
    //// TODO: 3/23/17 merge with state onEvent lookup
    if (block.handle(receivedSpec)) {
      return new Transition(receivedSpec, state);
    }
    Testkit.logger.debug("{}: looking up {} with block {}", state, receivedSpec, block.pendingEventsToString());

    for (EventSpec<? extends KompicsEvent> eventSpec : block.getAllowedEvents()) {
      if (eventSpec.match(receivedSpec)) {
        StateTable.Transition transition = new StateTable.Transition(eventSpec, Action.HANDLE, state);
        receivedSpec.handle();
        return transition;
      }
    }

    for (EventSpec<? extends KompicsEvent> eventSpec : block.getDroppedEvents()) {
      if (eventSpec.match(receivedSpec)) {
        return new StateTable.Transition(eventSpec, Action.DROP, state);
      }
    }

    for (EventSpec<? extends KompicsEvent> eventSpec : block.getDisallowedEvents()) {
      if (eventSpec.match(receivedSpec)) {
        return new StateTable.Transition(eventSpec, Action.FAIL, FSM.ERROR_STATE);
      }
    }

    return defaultLookup(state, receivedSpec);
  }

  private <E extends KompicsEvent> Action actionFor(
          KompicsEvent event, Class<? extends KompicsEvent> registeredType) {
    E ev = (E) event;
    Function<E, Action> function = (Function<E, Action>) defaultActions.get(registeredType);
    Action action = function.apply(ev);
    if (action == null) {
      throw new NullPointerException(String.format("(default handler for %s returned null for event '%s')",
              registeredType, event));
    }
    return action;
  }

  void printTable(int final_state) {
    for (int i = 0; i <= final_state; i++) {
      State state = states.get(i);
      if (state != null) {
        System.out.println(i);
        for (Transition t : state.transitions.values()) {
          System.out.println("\t\t" + t);
        }
        System.out.println("\t\t" + state);
      }
    }
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

    Transition(EventSpec eventSpec, int nextState) {
      this(eventSpec, Action.HANDLE, nextState);
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
      return "( " + eventSpec + " ) " + action + " " + nextState;
    }
  }

  private class State {
    private final int state;
    private final Block block;
    private PredicateSpec predicateSpec;
    private EventSpec<? extends KompicsEvent> eventSpec;
    private List<Spec> expectUnordered;

    private List<Spec> pending;
    private List<EventSpec<? extends KompicsEvent>> seen;
    private final Map<EventSpec<? extends KompicsEvent>, Transition> transitions =
            new HashMap<EventSpec<? extends KompicsEvent>, Transition>();

    private State(int state, Block block) {
      this.state = state;
      this.block = block;
      addTransitions(block);
    }

    State(int state, Spec spec, Block block) {
      this(state, block);
      if (spec instanceof EventSpec) {
        this.eventSpec = (EventSpec<? extends KompicsEvent>) spec;
      } else {
        this.predicateSpec = (PredicateSpec) spec;
      }
    }

    State(int state, List<Spec> expectUnordered, Block block) {
      this(state, block);
      this.expectUnordered = expectUnordered;
      seen = new ArrayList<EventSpec<? extends KompicsEvent>>(expectUnordered.size());
      pending = new ArrayList<Spec>(expectUnordered);
    }

    void addTransitions(Block block) {
      for (EventSpec<? extends KompicsEvent> e : block.getDisallowedEvents()) {
        addTransition(e, Action.FAIL, FSM.ERROR_STATE);
      }
      for (EventSpec<? extends KompicsEvent> e : block.getAllowedEvents()) {
        addTransition(e, Action.HANDLE, state);
      }
      for (EventSpec<? extends KompicsEvent> e : block.getDroppedEvents()) {
        addTransition(e, Action.DROP, state);
      }
    }

    private void addTransition(EventSpec<? extends KompicsEvent> onEvent, Action action, int nextState) {
      Transition transition = new Transition(onEvent, action, nextState);
      transitions.put(onEvent, transition);
    }

    Transition onEvent(EventSpec<? extends KompicsEvent> receivedSpec) {
      if (block.handle(receivedSpec)) {
        return new Transition(receivedSpec, state);
      }

      // single event or predicate transition
      if ((eventSpec != null && eventSpec.match(receivedSpec)) ||
          (predicateSpec != null && predicateSpec.match(receivedSpec))) {
        receivedSpec.handle();
        int nextState = state + 1;
        return new Transition(receivedSpec, nextState);
      }

      // unordered events
      if (expectUnordered != null && pending.contains(receivedSpec)) {
        int index = pending.indexOf(receivedSpec);
        seen.add(receivedSpec);
        pending.remove(index);

        if (!pending.isEmpty()) {
          return new Transition(receivedSpec, state);
        } else {
          for (EventSpec<? extends KompicsEvent> e : seen) {
            e.handle();
          }
          reset();
          int nextState = state + 1;
          return new Transition(receivedSpec, nextState);
        }
      }

      // other transitions
      Transition transition = transitions.get(receivedSpec);
      // default transition
      if (transition == null) {
        transition = defaultLookup(state, receivedSpec);
      }

      if (transition != null && transition.action == Action.HANDLE) {
        receivedSpec.handle();
      }

      return transition;
    }

    private void reset() {
      for (Spec spec : expectUnordered) {
        pending.add(spec);
      }
      seen.clear();
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (predicateSpec != null) {
        sb.append(predicateSpec.toString());
      } else if (eventSpec != null) {
        sb.append(eventSpec.toString());
      } else {
        sb.append("Unordered<Seen(");
        for (EventSpec e : seen) {
          sb.append(" ").append(e);
        }
        sb.append(")Pending(");
        for (Spec e : pending) {
          sb.append(" ").append(e);
        }
        sb.append(")>");
      }
      return sb.append(" handle ").append(state + 1).toString();
    }
  }

}
