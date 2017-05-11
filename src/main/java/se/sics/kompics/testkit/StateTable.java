/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.testkit;

import com.google.common.base.Function;
import se.sics.kompics.KompicsEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static se.sics.kompics.testkit.Action.DROP;

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

  void addTransition(int state, int nextState, Spec spec, Block block) {
    getState(state, block).addTransition(spec, nextState);
  }

  Transition performInternalTransition(int state, boolean ignoreOtherTransitions) {
    State stateObj = states.get(state);
    if (stateObj != null) {
      return stateObj.performInternalTransition(ignoreOtherTransitions);
    }
    return null;
  }

  private State getState(int state, Block block) {
    State stateObj = states.get(state);
    if (stateObj == null) {
      stateObj = new State(state, block);
      states.put(state, stateObj);
    }
    return stateObj;
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

  boolean hasState(int state) {
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
            return new Transition(Action.HANDLE, state);
          case DROP:
            return new Transition(DROP, state);
          default:
            return new Transition(Action.FAIL, FSM.ERROR_STATE);
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
    final Action action;
    final int nextState;
    final String errorMessage;

    Transition(Action action, int nextState) {
      this(action, nextState, null);
    }

    Transition(Action action, int nextState, String errorMessage) {
      this.action = action;
      this.nextState = nextState;
      this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
      return String.format("%s -> %s", action, nextState);
    }
  }

  class State {
    private final int state;
    private final Block block;

    InternalEventSpec internalSpec;
    Map<Spec, Integer> transitions = new HashMap<Spec, Integer>();

    private State(int state, Block block) {
      this.state = state;
      this.block = block;
    }

    void addTransition(Spec spec, int nextState) {
      transitions.put(spec, nextState);
      boolean internal = spec instanceof InternalEventSpec;
      if (internal) {
        internalSpec = (InternalEventSpec) spec;
      }
      //isInternalState = transitions.size() == 1 && internal;
    }

    Transition onEvent(EventSpec receivedSpec) {
      Transition transition = null;

      for (Spec spec : transitions.keySet()) {
        boolean matched = spec.match(receivedSpec);
        if (matched) {
          if (spec instanceof MultiEventSpec) {
            MultiEventSpec mSpec = (MultiEventSpec) spec;
            int nextState = state;
            if (mSpec.isComplete()) {
              nextState = transitions.get(spec);
            }
            transition = new Transition(DROP, nextState);
          } else {
            transition = new Transition(Action.HANDLE, transitions.get(spec));
          }
          break;
        }
      }

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

    Transition performInternalTransition(boolean ignoreOtherTransitions) {
      return internalSpec != null && (ignoreOtherTransitions || transitions.size() == 1)?
          doInternalTransition() : null;
    }

    // state merged with external event and internal events (eg {expect 2, trigger 3})
    Transition tryPerformInternalTransition() {
      if (internalSpec == null) {
        return null;
      }
      return doInternalTransition();
    }

    Transition doInternalTransition() {
      String errorMessage = internalSpec.performInternalEvent();
      int nextState = errorMessage == null? transitions.get(internalSpec) : FSM.ERROR_STATE;
      return new Transition(DROP, nextState, errorMessage);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("[");
      sb.append(state).append(" {");
      boolean first = true;
      for (Spec spec : transitions.keySet()) {
        if (!first) {
          sb.append(",");
        } else {
          first = false;
        }
        sb.append(spec).append(" -> ")
          .append(transitions.get(spec));
      }
      sb.append("}]");
      return sb.toString();
    }
  }

}
