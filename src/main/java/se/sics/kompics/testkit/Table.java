package se.sics.kompics.testkit;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import static se.sics.kompics.testkit.Action.*;

class Table {
  private int stateIDs = 0;

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

  private final NFA<? extends ComponentDefinition> nfa;
  private RepeatFA repeatMain;
  private Map<Integer, State> stateMap = new HashMap<Integer, State>();
  private Map<State, Integer> stateToId = new HashMap<State, Integer>();
  private State currentState;
  private final State errorState = new SingleState(nextid(), null, false);
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

  <E extends KompicsEvent> void setDefaultAction(Class<E> eventType, Function<E, Action> predicate) {
    defaultActions.put(eventType, predicate);
  }

  private Transition defaultLookup(EventSpec receivedSpec) {
    if (receivedSpec == null) {
      return null;
    }

    KompicsEvent event = receivedSpec.getEvent();
    Class<? extends KompicsEvent> eventType = event.getClass();

    for (Class<? extends KompicsEvent> registeredType : defaultActions.keySet()) {
      if (registeredType.isAssignableFrom(eventType)) {
        Action action = actionFor(event, registeredType);
        switch (action) {
          case HANDLE:
            return new Transition(receivedSpec, currentState, true);
          case DROP:
            return new Transition(receivedSpec, currentState);
          default:
            return new Transition(receivedSpec, errorState);
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
    currentFA.end();
    currentFA = faStack.peek(); // previous
    currentFA.addFA(child);
  }

  void build() {
    BaseFA finalFA = new BaseFA(repeatMain.block);
    assert repeatMain == currentFA;
    repeatMain.end();
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

    if (visited.size() < 10) {
      logger.error("visited = {}", visited);
      for (State s : visited) {
        logger.error("{}", s.show());
      }
      logger.error("map = {}", stateMap);
    }
  }

  boolean isInFinalState() {
    return currentState.isFinalState;
  }

  boolean doTransition(EventSpec receivedSpec) {
    logger.debug("{}: lookup event {}", stateToId.get(currentState), receivedSpec);
    while (true) {
      doInternalTransitions();

      Transition t = currentState.getTransition(receivedSpec);
      if (t == null) {
        t = defaultLookup(receivedSpec);
      }

      if (t != null) {
        currentState = t.nextState;
        logger.debug("Matched event {} with transition {}, {}", receivedSpec, t, stateToId.get(t.nextState));
        // handles all
        if (t.nextState == errorState) {
          logger.debug("Received unwanted event {} at state {}", receivedSpec, currentState);
          return false;
        } else if (t.handle) {
          receivedSpec.handle();
        }
        return true;
      }

      // internal actions - ignore other transitions
      t = currentState.getTransition(true);
      if (t != null) {
        currentState = t.nextState;
        continue;
      }

      // e transitions
      t = currentState.getTransition(EventSpec.EPSILON);
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
          logger.error("Last state was {}", currentState.show());
        }
      }
      return false;
    }
  }

  void doInternalTransitions() {
    Transition transition;
    do {
      transition = currentState.getTransition(true);
      if (transition != null) {
        currentState = transition.nextState;
      }
    } while (transition != null);
  }


  private class BaseFA extends FA{
    Spec spec;
    BaseFA(Spec spec, Block block, boolean lastStateInBlock) {
      super(block);
      startState = new SingleState(nextid(), block, lastStateInBlock);
      this.spec = spec;
    }

    BaseFA(Block block) {
      super(block);
      startState = new SingleState(nextid(), block, false);
      startState.isFinalState = true;
    }

    @Override
    void build(FA finalFA) {
      assert spec != null;
      startState.addTransition(new Transition(spec, finalFA.startState, true));
      if (spec instanceof InternalEventSpec) {
        ((SingleState) startState).internalEventSpec = (InternalEventSpec) spec;
        return; // no need for event transitions
      }
      for (EventSpec spec : block.getAllowedSpecs()) {
        startState.addTransition(new Transition(spec, startState, true));
      }
      for (EventSpec spec : block.getDroppedSpecs()) {
        startState.addTransition(new Transition(spec, startState));
      }
      for (EventSpec spec : block.getDisallowedSpecs()) {
        startState.addTransition(new Transition(spec, errorState));
      }
    }

    @Override
    public String toString() {
      return "BaseFA " + startState;
    }
  }

  private class KleeneStar extends FA {

    KleeneStar(Block block) {
      super(block);
    }

    @Override
    void addSpec(Spec spec) {
      children.add(new BaseFA(spec, block, false));
    }

    @Override
    void build(FA finalFA) {
      FA next = children.get(children.size() - 1);
      FA current;
      for (int i = children.size() - 2; i >= 0; i--) {
        current = children.get(i);
        current.build(next);
        next = current;
      }

      startState = children.get(0).startState;
      mergeWith(finalFA);
    }

    private void mergeWith(FA finalFA) {
      
    }

    @Override
    void end() {
      FA blockEnd = new BaseFA(EventSpec.EPSILON, block, true);
      children.add(blockEnd);
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
      children.add(new BaseFA(spec, block, false));
    }

    @Override
    void build(FA finalFA) {
      // original
      int numChildren = children.size();
      cloneChildren();

      FA next = null;
      FA current;
      for (int i = children.size() - 1; i >= 0; i--) {
        current = children.get(i);
        current.build(next != null? next : finalFA);
        next = current;
      }

      startState = children.get(0).startState;
      // only original start is block start
      startState.isBlockStart = true;

      // original start + clones are iteration start
      for (int i = 0; i < children.size(); i+=numChildren) {
        children.get(i).startState.isIterationStart = true;
      }

      // if first statement is not baseFA then start state
      // belongs to some other block
      // i.e first statement in body is repeat(...)
      // so add block to be initialized on entry to start state
      if (!(children.get(0) instanceof BaseFA)) {
        startState.addPreceedingBlockInit(block);
      }
    }

    @Override
    void end() {
      FA blockEnd = new BaseFA(EventSpec.EPSILON, block, true);
      children.add(blockEnd);
    }

    private void cloneChildren() {
      if (count <= 1) {
        return;
      }
      int numChildren = children.size();
      try {
        for (int i = 0; i < count - 1; i++) {
          for (int j = 0; j < numChildren; j++) {
            children.add((FA) children.get(j).clone());
          }
        }
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }
  }


  abstract class FA implements Cloneable{
    State startState;
    Collection<State> endStates;
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

      List<State> endStatesClone = new ArrayList<State>();
      for (State endState : endStates) {
        endStatesClone.add((State) endState.clone());
      }
      clone.endStates = endStatesClone;

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

    void end() { }
  }

  private class SingleState extends State {

    InternalEventSpec internalEventSpec;
    SingleState(int number, Block block, boolean isBlockEnd) {
      super(block);
      this.isBlockEnd = isBlockEnd;
      id = new ID(number);
    }

    @Override
    void addTransition(Transition t) {
      transitions.put(t.spec, t);
    }

    @Override
    Transition intermediateStateTransition(boolean ignoreOtherTransitions) {
      Transition t = transitions.get(internalEventSpec);
      if (t != null) {
        internalEventSpec.performInternalEvent();
      }
      return t;
    }

    @Override
    void runEntryExitFunctions(EventSpec receivedSpec, Transition t) {
      if (isBlockStart) {
        for (Block b : preceedingBlockInits) {
          b.runBlockInit();
          b.runIterationInit();
        }
        block.runBlockInit();
      }

      if (isIterationStart) {
        logger.error("{}, {} -> running iteration init", this, stateToId.get(this));
        block.runIterationInit();
      }
    }
  }

  class MergedState extends State {
    final Set<State> children = new HashSet<State>();
    // track which sub state has transitions for an event
    private Multimap<Spec, State> specToChildStates = HashMultimap.<Spec, State>create();

    MergedState(Collection<State> childStates, Block block) {
      super(block);

      Set<Integer> ids = new HashSet<Integer>();
      for (State child : childStates) {
        // final state if child state is final
        if (child.isFinalState) {
          isFinalState = true;
        }

        for (Spec s : child.transitions.keySet()) {
          specToChildStates.put(s, child);
        }

        // ids
        for (int cID : child.id.ids) {
          ids.add(cID);
        }
        children.add(child);
      }
      id = new ID(ids);
    }

    @Override
    void addTransition(Transition t) {
      throw new UnsupportedOperationException("");
    }

    @Override
    Transition intermediateStateTransition(boolean ignoreOtherTransitions) {
      throw new UnsupportedOperationException("");
    }

    @Override
    void runEntryExitFunctions(EventSpec receivedSpec, Transition t) {
      if (t == null) {
        return;
      }

      if (onEntry) {
        onEntry = false;
      }

      for (State child : specToChildStates.get(receivedSpec)) {
        child.runEntryExitFunctions(receivedSpec, t);
      }

      if (t.nextState != this) {
        onEntry = true;
      }
    }
  }

  abstract class State implements Cloneable{
    ID id;
    final Block block;
    Map<Spec, Transition> transitions = new HashMap<Spec, Transition>();

    List<Block> preceedingBlockInits = new ArrayList<Block>();
    boolean isFinalState;
    boolean isBlockEnd;
    boolean isBlockStart;
    boolean isIterationStart;
    boolean onEntry = true;

    State(Block block) {
      this.block = block;
    }

    abstract void addTransition(Transition t);

    Transition getTransition(EventSpec receivedSpec) {
      Transition t = null;
      if (block.handle(receivedSpec)) {
        t = new Transition(receivedSpec, this);
      } else {
        for (Map.Entry<Spec, Transition> entry : transitions.entrySet()) {
          Spec spec = entry.getKey();
          if (spec.match(receivedSpec)) {
            if (spec instanceof MultiEventSpec) {
              State nextState = this;
              if (((MultiEventSpec) spec).isComplete()) {
                nextState = entry.getValue().nextState;
              }
              t = new Transition(receivedSpec, nextState);
            } else {
              t = entry.getValue();
            }
          }
        }
      }
      runEntryExitFunctions(receivedSpec, t);
      return t;
    }

    Transition getTransition(boolean ignoreOtherTransitions) {
      Transition t = isBlockEnd
          ? blockEndTransition()
          : intermediateStateTransition(ignoreOtherTransitions);
      runEntryExitFunctions(EventSpec.EPSILON, t);
      return t;
    }

    abstract Transition intermediateStateTransition(boolean ignoreOtherTransitions);
/*    Transition intermediateStateTransition(boolean ignoreOtherTransitions) {
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
    }*/

    private Transition blockEndTransition() {
      //logger.error("{}: LAST STATE -> block status = {}", this, block.status());
      if (!block.hasPendingEvents()) {
        block.iterationComplete();
        return transitions.get(EventSpec.EPSILON);
      }
      return null;
    }

    abstract void runEntryExitFunctions(EventSpec receivedSpec, Transition t);
/*
    private void runEntryExitFunctions(EventSpec receivedSpec, Transition t) {
      //logger.error("{}, block = {}", this, block.hashCode());
      if (t == null) {
        return;
      }

      if (onEntry) {
        onEntry = false;
        if (isMergedState) {
          for (State state : specToChildStates.get(receivedSpec)) {
            state.runEntryExitFunctions(receivedSpec, t);
          }
        } else {
          if (isBlockStart) {
            for (Block b : preceedingBlockInits) {
              b.runBlockInit();
              b.runIterationInit();
            }
            block.runBlockInit();
          }
          if (isIterationStart) {
            logger.error("{}, {} -> running iteration inits", this, stateToId.get(this));
            block.runIterationInit();
          }
        }
      }

      if (t.nextState != this) { // exit
        onEntry = true;
      }
    }
*/

    private void addPreceedingBlockInit(Block preceedingBlock) {
      preceedingBlockInits.add(preceedingBlock);
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

  private class Transition implements Cloneable{
    final Spec spec;
    final State nextState;
    //final Action action;
    boolean handle;

    Transition(Spec spec, State nextState) {
      this.spec = spec;
      this.nextState = nextState;
    }
    Transition(Spec spec, State nextState, boolean handle) {
      this(spec, nextState);
      this.handle = handle;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Transition)) {
        return false;
      }
      Transition other = (Transition) o;
      return spec.equals(other.spec);// && nextState.equals(other.nextState);
    }

    @Override
    public int hashCode() {
      return 31 * spec.hashCode();
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
