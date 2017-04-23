package se.sics.kompics.testkit;

import org.slf4j.Logger;
import se.sics.kompics.ComponentDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static se.sics.kompics.testkit.Action.*;

class Table {
  private int stateIDs = 0;

  private final NFA<? extends ComponentDefinition> nfa;
  private RepeatFA repeatMain;
  private Map<Integer, State> stateMap = new HashMap<Integer, State>();
  private Map<State, Integer> stateToId = new HashMap<State, Integer>();
  private State currentState;
  private Logger logger = Testkit.logger;
  private FA currentFA;
  private Stack<FA> faStack = new Stack<FA>();

  Table(NFA<? extends ComponentDefinition> nfa, Block initialBlock) {
    this.nfa = nfa;
    repeatMain = new RepeatFA(1, initialBlock);
    currentFA = repeatMain;
    faStack.push(repeatMain);
  }

  private int nextid() {
    return stateIDs++;
  }

  void addSpec(Spec spec) {
    currentFA.addSpec(spec);
  }

  void addRepeat(int count, Block block) {
    currentFA = new RepeatFA(count, block);
    faStack.push(currentFA);
  }

  void endRepeat() {
    FA child = faStack.pop();
    assert child == currentFA;
    if (faStack.isEmpty()) {
      throw new IllegalStateException("no matching block for end repeat");
    }
    currentFA = faStack.peek();
    currentFA.addFA(child);
  }

  void build() {
    BaseFA finalFA = new BaseFA(repeatMain.block);
    assert repeatMain == currentFA;
    repeatMain.build(finalFA);
    assignNumbersToStates();
    currentState = repeatMain.startState;
  }

  private void assignNumbersToStates() {
    HashSet<State> visited = new HashSet<State>();
    LinkedList<State> pending = new LinkedList<State>();
    pending.add(repeatMain.startState);

    int stateNum = 0;
    State currentState;
    while (!pending.isEmpty()) {
      currentState = pending.removeFirst();
      //logger.error("{}", currentState.show());
      visited.add(currentState);
      int stateID = stateNum++;
      stateMap.put(stateID, currentState);
      stateToId.put(currentState, stateID);
      for (Transition t : currentState.transitions.values()) {
        State adj = t.nextState;
        if (!(visited.contains(adj) || pending.contains(adj))) {
          pending.add(adj);
        }
      }
    }

    logger.error("visited = {}", visited);
    for (State s : visited) {
      logger.error("{}", s.show());
    }
    logger.error("map = {}", stateMap);
  }

  boolean isInFinalState() {
    return currentState.isFinalState;
  }

  boolean doTransition(EventSpec receivedSpec) {
    logger.debug("{}: lookup event {}", stateToId.get(currentState), receivedSpec);
    while (true) {
      doInternalTransitions();

      Transition t = currentState.doTransition(receivedSpec);
      if (t != null) {
        currentState = t.nextState;
        logger.debug("Matched event {} with transition {}, {}", receivedSpec, t, stateToId.get(t.nextState));
        // handles all
        switch (t.action) {
          case HANDLE:
            receivedSpec.handle();
            break;
          case FAIL:
            logger.debug("Received unwanted event {} at state {}", receivedSpec, currentState);
            return false;
        }
        return true;
      }

      // internal actions - ignore other transitions
      t = currentState.doTransition(true);
      if (t != null) {
        currentState = t.nextState;
        continue;
      }

      // e transitions
      t = currentState.doTransition(EventSpec.EPSILON);
      if (t != null) {
        currentState = t.nextState;
        continue;
      }

      if (isInFinalState()) {
        logger.error("final state");
      } else {
        if (receivedSpec == null) {
          logger.error("No event received");
        } else {
          logger.error("No transitions found for {}", receivedSpec);
          logger.error("Last state was {}", currentState);
        }
      }
      return false;
    }
  }

  void doInternalTransitions() {
    Transition transition;
    do {
      transition = currentState.doTransition(true);
      if (transition != null) {
        currentState = transition.nextState;
      }
    } while (transition != null);
  }


  private class BaseFA extends FA{
    Spec spec;
    BaseFA(Spec spec, Block block) {
      super(block);
      startState = new State(nextid());
      this.spec = spec;
    }

    BaseFA(Block block) {
      super(block);
      startState = new State(nextid());
      startState.isFinalState = true;
    }

    @Override
    void build(FA finalFA) {
      assert spec != null;
      startState.addTransition(spec, finalFA.startState, HANDLE);
      if (spec instanceof InternalEventSpec) {
        return; // no need for event transitions
      }
      for (EventSpec spec : block.getAllowedSpecs()) {
        startState.addTransition(spec, startState, HANDLE);
      }
      for (EventSpec spec : block.getDroppedSpecs()) {
        startState.addTransition(spec, startState, DROP);
      }
      for (EventSpec spec : block.getDisallowedSpecs()) {
        startState.addTransition(spec, startState, FAIL);
      }
    }

    @Override
    public String toString() {
      return "BaseFA " + startState;
    }
  }

  private class RepeatFA extends FA {
    final int count;
    RepeatFA(int count, Block block) {
      super(block);
      assert count >= 1;
      this.count = count;
    }

    @Override
    void addSpec(Spec spec) {
      children.add(new BaseFA(spec, block));
    }

    @Override
    void build(FA finalFA) {
      cloneChildren();
      FA next = null; // next FA in sequence
      FA current;
      for (int i = children.size() - 1; i >= 0; i--) {
        current = children.get(i);
        current.build(next != null? next : finalFA);
        next = current;
      }
      if (children.isEmpty()) {
        startState = finalFA.startState;
      } else {
        startState = children.get(0).startState;
      }

      assert !children.isEmpty();
    }

    private void cloneChildren() {
      if (count <= 1) {
        return;
      }

      List<FA> clones = new ArrayList<FA>();
      for (FA child : children) {
        clones.add(child);
      }
      try {
        for (int i = 0; i < count - 1; i++) {
          for (FA child : children) {
            clones.add((FA) child.clone());
          }
        }
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
        children = clones;
    }
  }


  abstract class FA implements Cloneable{
    State startState;
    State endState;
    List<FA> children = new ArrayList<FA>();
    Set<State> states = new HashSet<State>();
    final Block block;

    FA(Block block) {
      this.block = block;
    }

    abstract void build(FA finalFA);

    void addSpec(Spec spec) {
      throw new UnsupportedOperationException();
    }

    final void addFA(FA childFA) {
      children.add(childFA) ;
    }

    protected Object clone() throws CloneNotSupportedException {
      FA clone = (FA) super.clone();
      clone.startState = startState == null? null : (State) clone.startState.clone();
      clone.endState = endState == null? null : (State) clone.endState.clone();

      List<FA> chldrn = new ArrayList<FA>();
      for (FA child : children) {
        chldrn.add((FA) child.clone());
      }
      clone.children = chldrn;

      Set<State> sts = new HashSet<State>();
      for (State state : states) {
        sts.add((State) state.clone());
      }
      clone.states = sts;

      return clone;
    }
  }

  class State implements Cloneable{
    final Set<State> children = new HashSet<State>();
    final ID id;
    Map<Spec, Transition> transitions = new HashMap<Spec, Transition>();
    boolean isFinalState;

    State(Collection<State> childStates) {
      Set<Integer> ids = new HashSet<Integer>();
      for (State child : childStates) {
        if (child.isFinalState) {
          isFinalState = true;
        }
        for (int cID : child.id.ids) {
          ids.add(cID);
        }
        children.add(child);
      }
      id = new ID(ids);
    }

    State(int number) {
      id = new ID(number);
    }

    void addTransition(Spec spec, State nextState, Action action) {
      Transition t = new Transition(spec, nextState, action);
      transitions.put(t.spec, t);
    }

    Transition doTransition(Spec spec) {
      return transitions.get(spec);
    }

    Transition doTransition(boolean ignoreOtherTransitions) {
      InternalEventSpec internalSpec = null;
      Transition internalTransition = null;
      for (Map.Entry<Spec, Transition> entry : transitions.entrySet()) {
        if (entry.getKey() instanceof InternalEventSpec) {
          internalSpec = (InternalEventSpec) entry.getKey();
          internalTransition = entry.getValue();
          break;
        }
      }
      Transition t = internalSpec != null && (transitions.size() == 1 || ignoreOtherTransitions)
          ? internalTransition : null;

      if (t != null) {
        t.performAction();
      }
      return t;
    }

    @Override
    public String toString() {
      return id.toString();
    }

    String show() {
      StringBuilder sb = new StringBuilder(id.toString());
      sb.append(transitions.values());
      return sb.toString();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
      State clone = (State) super.clone();
      Map<Spec, Transition> t = new HashMap<Spec, Transition>();
      for (Map.Entry<Spec, Transition> entry : transitions.entrySet()) {
        t.put(entry.getKey(), entry.getValue());
      }
      clone.transitions = t;
      return clone;
    }
  }

  class Transition implements Cloneable{
    final Spec spec;
    final State nextState;
    final Action action;

    Transition(Spec spec, State nextState, Action action) {
      this.spec = spec;
      this.nextState = nextState;
      this.action = action;
    }

    void performAction() {
      // if has loop start -> run
      // if has loop end -> run
      // if has internal event -> run
      if (spec instanceof InternalEventSpec) {
        ((InternalEventSpec) spec).performInternalEvent();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Transition)) {
        return false;
      }
      Transition other = (Transition) o;
      return spec.equals(other.spec); //&& nextState.equals(other.nextState);
    }

    @Override
    public int hashCode() {
      int result = 31 * spec.hashCode();
      result += 31 * nextState.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return spec + "->" + nextState;
    }

  }

  private class ID implements Cloneable{
    private final Set<Integer> ids;
    ID(Set<Integer> ids) {
      this.ids = ids;
    }

    ID(int id) {
      ids = new HashSet<Integer>(id);
      ids.add(id);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("{");
      boolean first = true;
      for (int id : ids) {
        if (!first) {
          sb.append(",");
        } else {
          first = false;
        }
        sb.append(id);
      }
      sb.append("}");
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof ID && ids.equals(((ID) o).ids);
    }

    @Override
    public int hashCode() {
      int sum = 0;
      for (int id : ids) sum += id;
      return sum;
    }

  }

}
