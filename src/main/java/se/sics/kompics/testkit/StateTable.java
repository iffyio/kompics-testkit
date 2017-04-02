package se.sics.kompics.testkit;

import com.google.common.base.Function;
import se.sics.kompics.KompicsEvent;

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

  Map<Integer, State> states = new HashMap<Integer, State>();

  void registerExpectedEvent(int state, Spec spec, Block block) {
    states.put(state, new State(state, spec, block));
  }

  void registerExpectedEvent(int state, List<SingleEventSpec> expectUnordered, Block block) {
    states.put(state, new State(state, expectUnordered, block));
  }

  <E extends KompicsEvent> void setDefaultAction(Class<E> eventType, Function<E, Action> predicate) {
    defaultActions.put(eventType, predicate);
  }

  void printExpectedEventAt(int state) {
    FSM.logger.debug("{}: Expect\t[{}]", state, getExpectedSpecAt(state));
  }

  String getExpectedSpecAt(int state) {
    return states.get(state).toString();
  }

  boolean isExpectState(int state) {
    return states.containsKey(state);
  }

  Transition lookup(int state, EventSpec receivedSpec) {
    return states.get(state).onEvent(receivedSpec);
  }

  Transition lookup(int state, EventSpec receivedSpec, Block block) {
    Transition transition = block.getTransition(state, receivedSpec);
    return transition != null? transition : defaultLookup(state, receivedSpec);
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

  private <E extends KompicsEvent> Action actionFor(
          KompicsEvent event, Class<? extends KompicsEvent> registeredType) {
    Function<E, Action> function = (Function<E, Action>) defaultActions.get(registeredType);
    Action action = function.apply((E) event);
    if (action == null) {
      throw new NullPointerException(String.format("(default handler for %s returned null for event '%s')",
              registeredType, event));
    }
    return action;
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

    // transitions are for same events are equal
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Transition)) {
        return false;
      }
      Transition other = (Transition) obj;
      return this.eventSpec.equals(other.eventSpec);
    }

    @Override
    public int hashCode() {
      return eventSpec.hashCode();
    }

    @Override
    public String toString() {
      return String.format("( %s ) %s %s", eventSpec, action, nextState);
    }
  }

  class State {
    private final int state;
    private final Block block;
    private Spec spec;

    final Map<EventSpec, Transition> transitions =
            new HashMap<EventSpec, Transition>();

    private State(int state, Block block) {
      this.state = state;
      this.block = block;
    }

    State(int state, Spec spec, Block block) {
      this(state, block);
      this.spec = spec;
    }

    State(int state, List<SingleEventSpec> expectUnordered, Block block) {
      this(state, block);
      spec = new UnorderedSpec(expectUnordered);
    }

    Transition onEvent(EventSpec receivedSpec) {
      Transition transition = spec.getTransition(receivedSpec, state);
      if (transition == null) {
        transition = block.getTransition(state, receivedSpec);
      }
      if (transition == null) {
        transition = defaultLookup(state, receivedSpec);
      }
      if (transition != null  && transition.action == Action.HANDLE) {
        receivedSpec.handle();
      }
      return transition;
    }

    public String toString() {
      String sb = "( " + spec + " ) " +
              Action.HANDLE + " " + (state + 1);
      return sb;
    }
  }

}
