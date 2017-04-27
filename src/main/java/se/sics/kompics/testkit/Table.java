package se.sics.kompics.testkit;

import com.google.common.base.Function;
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

  private RepeatFA repeatMain;
  private Set<State> currentStates = new HashSet<State>();
  private final State errorState = new State(nextid(), null);
  private Logger logger = Testkit.logger;
  private FA currentFA;
  private Stack<FA> faStack = new Stack<FA>();

  Table(Block initialBlock) {
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

  private Action defaultLookup(EventSpec receivedSpec) {
    if (receivedSpec == null) {
      return null;
    }

    KompicsEvent event = receivedSpec.getEvent();
    Class<? extends KompicsEvent> eventType = event.getClass();

    for (Class<? extends KompicsEvent> registeredType : defaultActions.keySet()) {
      if (registeredType.isAssignableFrom(eventType)) {
        return actionFor(event, registeredType);
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
    assert repeatMain == currentFA;
    repeatMain.end();
    BaseFA finalFA = new BaseFA(repeatMain.block);
    repeatMain.build(finalFA);
    //assignNumbersToStates();
    currentStates = repeatMain.startState.eclosure();
    Testkit.logger.debug("start state = {}", currentStates);
    for (State s : currentStates) {
      logger.debug("{}", s.show());
    }
    Testkit.logger.debug("final state = {}", finalFA.startState);
  }

  private void showCurrentState() {
    HashSet<State> visited = new HashSet<State>();
    LinkedList<Set<State>> pending = new LinkedList<Set<State>>();
    pending.add(currentStates);
    for (State state : currentStates) {
      logger.debug("{}[{}]", state, state.transitions);
    }
  }

  boolean isInFinalState() {
    for (State state : currentStates) {
      if (state.isFinalState) {
        return true;
      }
    }
    return false;
  }

  boolean doTransition(EventSpec receivedSpec) {
    logger.debug("{}: lookup event {}", currentStates, receivedSpec);
    while (true) {
      tryInternalEventTransitions();

      // for each current state, get next state for spec
      Set<State> nextStates = new HashSet<State>();
      Set<Transition> transitions = new HashSet<Transition>();
      for (State state : currentStates) {
        Transition t = state.getTransition(receivedSpec);
        if (t != null) {
          nextStates.add(t.nextState);
          transitions.add(t);
        }
      }


      if (!nextStates.isEmpty()) {
        //logger.debug("{} some transitions were found from current state", currentStates);
        // kill threads without transitions and set new current states to next states
        updateCurrentState(nextStates);

        // handle received spec at most once
        for (Transition tr : transitions) {
          if (tr.handle) {
            receivedSpec.handle();
            break;
          }
        }
        return true;

      } else {
        //logger.debug("{} NO transitions were found from current state", currentStates);
        //logger.debug("{} forcing internal event transition");
        // check if any current state is an internal action
        // if found kill those that aren't and retry handle received spec
        forceInternalEventTransitions(nextStates);
        if (!nextStates.isEmpty()) {
          logger.debug("internal transition(s) found");
          updateCurrentState(nextStates);
          continue;
        }
        logger.debug("No internal transition(s) found");
        logger.debug("trying e-transitions");

        // no transitions found yet
        // try e-transitions from each current state
        // if found, kill those without and retry handle received spec
        performEpsilonTransitions(nextStates);
        if (!nextStates.isEmpty()) {
          //logger.debug("e-transitions were found");
          updateCurrentState(nextStates);
          continue;
        }
        //logger.debug("NO e-transitions were found");
      }

      //logger.debug("Checking default action for event");
      // try registered default actions
      boolean handleByDefault = tryDefaultActions(receivedSpec);
      if (handleByDefault) {
        //logger.debug("event was handled per default");
        return true;
      }

      //logger.error("No transitions were found for event!");
      if (receivedSpec == null) {
        logger.debug("No event was received");
      } else {
        logger.error("No transitions found for {}", receivedSpec);
        logger.debug("Last state was {}", currentStates);
      }
      return false;
    }
  }

  private boolean tryDefaultActions(EventSpec receivedSpec) {
    Action action = defaultLookup(receivedSpec);
    if (action == null) {
      //logger.debug("no default action found");
      return false;
    }

    //logger.debug("default action {}", action));
    switch (action) {
      case FAIL:
        logger.debug("Received unwanted event {} at state {}", receivedSpec, currentStates);
        return false;
      case HANDLE:
        receivedSpec.handle();
      default:
        return true;
    }
  }

  private void performEpsilonTransitions(Set<State> nextStates) {
    assert nextStates.isEmpty();
    for (State state : currentStates) {
      if (state.canPerformInternalTransition()) {
        Transition t = state.getTransition(EventSpec.EPSILON);
        nextStates.add(t.nextState);
      }
    }
  }

  private void forceInternalEventTransitions(Set<State> nextStates) {
    assert nextStates.isEmpty();
    for (State state : currentStates) {
      if (state.canPerformInternalTransition()) {
        Transition t = state.doInternalEventTransition();
        nextStates.add(t.nextState);
      }
    }
  }

  void tryInternalEventTransitions() {
    //logger.debug("{}: trying internal event", currentStates);
    while (true) {
      // if some thread in the NFA expects an event, do nothing
      for (State state : currentStates) {
        if (!state.canPerformInternalTransition()) {
          //logger.debug("state {} has no internal event. returning", state);
          return;
        }
      }

      // all current states have internal event specs (trigger, inspect etc) -
      // perform them
      // if so, non-singular set currentStates implies an ambiguous test specification
      Set<State> nextStates = new HashSet<State>();
      for (State state : currentStates) {
        logger.debug("{}", state);
        Transition t = state.doInternalEventTransition();
        assert t != null;
        nextStates.add(t.nextState);
      }
      assert !nextStates.isEmpty();
      //logger.debug("all states have internal transitions");
      updateCurrentState(nextStates);
    }
  }

  private void updateCurrentState(Set<State> nextStates) {
    logger.debug("{}: new state is {}", currentStates, nextStates);
    // for each thread discontinued by NFA -
    // reset events seen within block
    // get discontinued states in and reset their blocks
    Set<Block> discontinued = new HashSet<Block>();
    Set<Block> active = new HashSet<Block>();
    for (State state : nextStates) {
      active.add(state.block);
    }

    for (State state : currentStates) {
      if (!nextStates.contains(state)) {
        discontinued.add(state.block);
      }
    }

    //logger.debug("discontinued {}, active {}", discontinued, active);

    discontinued.removeAll(active);
    for (Block block : discontinued) {
      logger.debug("reseting {}", block);
      block.reset();
    }

    // update current state
    currentStates = nextStates;
  }


  private class BaseFA extends FA{
    Spec spec;
    BaseFA(Spec spec, Block block) {
      super(block);
      startState = new State(nextid(), block);
      this.spec = spec;
    }

    BaseFA(Spec spec, Block block, State startState) {
      super(block);
      this.startState = startState;
      this.spec = spec;
    }

    BaseFA(Block block) {
      super(block);
      startState = new State(nextid(), block);
      startState.isFinalState = true;
    }

    @Override
    void build(FA finalFA) {
      assert spec != null;
      startState.addTransition(new Transition(spec, finalFA.startState, true));
      if (spec instanceof InternalEventSpec) {
        State s = startState;
        s.internalEventSpec = (InternalEventSpec) spec;
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
      if (children.isEmpty()) {
        State repeatStartState = new State(nextid(), block);
        repeatStartState.isRepeatStart = true;
        children.add(new BaseFA(spec, block, repeatStartState));
      } else {
        children.add(new BaseFA(spec, block));
      }
    }

    @Override
    void build(FA finalFA) {
      FA endFA = children.get(children.size() - 1);
      State endState = endFA.startState;
      assert endState.isRepeatEnd;
      endState.exitTransition = new Transition(EventSpec.EPSILON, finalFA.startState);

      FA next = endFA;
      FA current;
      for (int i = children.size() - 2; i >= 0; i--) { // ignore repeatend
        current = children.get(i);
        current.build(next);
        next = current;
      }

      startState = children.get(0).startState;
      startState.isRepeatStart = true;
      //assert startState.isRepeatStart;
      //assert startState instanceof RepeatStartState;
      endState.loopTransition = new Transition(EventSpec.EPSILON, startState);

      // if repeat has a nested start state 's' belonging to another block
      // register block to be run on entry to 's'
      if (!(children.get(0) instanceof BaseFA)) {
        assert startState.isRepeatStart;
        startState.parentBlocks.add(0, block);
      }
    }

    @Override
    void end() {
      State repeatend = new State(nextid(), block);
      repeatend.isRepeatEnd = true;
      FA blockEnd = new BaseFA(EventSpec.EPSILON, block, repeatend);
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


  class State implements Cloneable{
    final ID id;
    final Block block;
    Map<Spec, Transition> transitions = new HashMap<Spec, Transition>();

    boolean isFinalState;
    boolean isRepeatStart;
    boolean isRepeatEnd;
    InternalEventSpec internalEventSpec;
    List<Block> parentBlocks = new LinkedList<Block>();

    Set<State> eclosure;

    Transition loopTransition;
    Transition exitTransition;
    Transition selfTransition = new Transition(EventSpec.EPSILON, this);

    State(int number, Block block) {
      this.block = block;
      id = new ID(number);
    }

    void addTransition(Transition t) {
      transitions.put(t.spec, t);
    }

    Transition getTransition(EventSpec receivedSpec) {
      if (isRepeatEnd && !block.hasPendingEvents()) {
        return null;
      }

      Transition t = null;
      if (block.handle(receivedSpec)) {
        t = new Transition(receivedSpec, this, true); // handle received event
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
      if (t == null) {
        t = handleWithBlockTransitions(receivedSpec);
      }

      if (isRepeatStart && t != null) {
        runBlockInits();
      }

      return t;
    }

    private Transition handleWithBlockTransitions(EventSpec receivedSpec) {
      Testkit.logger.debug("{}: looking up {} with block {}", this, receivedSpec, block.status());
      if (block.getAllowedSpecs().contains(receivedSpec)) {
        return new Transition(receivedSpec, this, true);
      }
      if (block.getDroppedSpecs().contains(receivedSpec)) {
        return new Transition(receivedSpec, this);
      }
      if (block.getDisallowedSpecs().contains(receivedSpec)) {
        return new Transition(receivedSpec, errorState);
      }
      return null;
    }

    Transition doInternalEventTransition() {
      if (isRepeatStart) {
        runBlockInits();
      }
      if (internalEventSpec != null) {
        internalEventSpec.performInternalEvent();
        return transitions.get(internalEventSpec);
      }
      if (isRepeatEnd) {
        return getRepeatEndTransition();
      }
      return null;
    }

    private boolean canPerformInternalTransition() {
      if (isRepeatEnd) {
        return !block.hasPendingEvents();
      }
      return internalEventSpec != null;
      //return internalEventSpec != null || isRepeatEnd;
    }

    private void runBlockInits() {
      if (!block.currentlyExecuting) {
        boolean notExecuting = false; // if a parent block is closed, all others nested must have been closed as well
        for (Block parent : parentBlocks) {
          if (notExecuting) {
            assert !parent.currentlyExecuting;
          }
          if (!parent.currentlyExecuting) {
            notExecuting = true;
            //logger.debug("{}, initializing parent {}", currentStates, parent);
            logger.debug("init {}", parent);
            parent.initialize();
          } else if (!parent.iterationInitHasRun) {
            logger.debug("iteration init {}", parent);
            parent.runIterationInit();
          }
        }
        logger.debug("init {}", block);
        //logger.debug("{}, block initialize", currentStates);
        block.initialize();
      } else {
        if (!block.iterationInitHasRun) {
          for (Block parent : parentBlocks) {
            if (!parent.iterationInitHasRun) {
              logger.debug("iteration init {}", parent);
              parent.runIterationInit();
            }
          }
          logger.debug("iteration init {}", block);
          block.runIterationInit();
        }
      }
    }

    private Transition getRepeatEndTransition() {
      //logger.debug("loop = {}, exit = {}", loopTransition, exitTransition);
      if (!block.hasPendingEvents()) {
        logger.debug("end{} count = {}", block, block.getCurrentCount());
        assert block.iterationInitHasRun; // iterationInit must have been run on this iteration
        block.iterationComplete();
        assert !block.iterationInitHasRun; // reset flag for next iteration

        assert loopTransition != null;
        assert exitTransition != null;
        //logger.debug("{}, block iterations? = {}", currentStates, block.hasMoreIterations());

        if (block.hasMoreIterations()) {
          return loopTransition;
        } else {
          // close block on exit
          block.currentlyExecuting = false;

          return exitTransition;
        }
      }
      return selfTransition;
    }

    private Transition blockEndTransition() {
      //logger.error("{}: LAST STATE -> block status = {}", this, block.status());
      if (!block.hasPendingEvents()) {
        block.iterationComplete();
        return transitions.get(EventSpec.EPSILON);
      }
      return null;
    }

    Set<State> eclosure() {
      if (eclosure != null) {
        return eclosure;
      }
      Set<State> eclose = new HashSet<State>();
      LinkedList<State> pending = new LinkedList<State>();
      pending.add(this);
      while (!pending.isEmpty()) {
        State current = pending.removeFirst();
        eclose.add(current);

        for (Map.Entry<Spec, Transition> entry : current.transitions.entrySet()) {
          State s = entry.getValue().nextState;
          if (entry.getKey() == EventSpec.EPSILON && !(eclose.contains(s) || pending.contains(s))) {
            pending.add(s);
          }
        }
      }
      eclosure = eclose;
      return eclosure;
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
    boolean handle;
    boolean isBlockTransition;

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
