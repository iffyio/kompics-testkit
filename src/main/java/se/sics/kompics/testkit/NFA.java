package se.sics.kompics.testkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import org.slf4j.Logger;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.PortType;
import se.sics.kompics.Port;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Start;

import static se.sics.kompics.testkit.Block.MODE;
import static se.sics.kompics.testkit.Block.MODE.*;

class NFA<T extends ComponentDefinition> {
  static final Logger logger = Testkit.logger;

  private final T definitionUnderTest;
  private final EventQueue eventQueue;

/*  static final int ERROR_STATE = -1;
  private String ERROR_MESSAGE = "";
  private int FINAL_STATE;
  private boolean STARTED = false;*/
  private boolean STARTED = false;

  private final ComponentCore proxyComponent;
  private Collection<Component> participants = new HashSet<Component>();

/*  private final Stack<Block> balancedBlock = new Stack<Block>();
  private Map<Integer, Block> blockStart = new HashMap<Integer, Block>();
  private Map<Integer, Block> blockEnd = new HashMap<Integer, Block>();

  private List<SingleEventSpec> expectUnordered = new ArrayList<SingleEventSpec>();
  private ExpectMapper expectMapper;
  private ExpectFuture expectFuture;*/

  private ExpectMapper expectMapper;
  private ExpectFuture expectFuture;
  private List<SingleEventSpec> expectUnordered = new ArrayList<SingleEventSpec>();
  private ComparatorMap comparators = new ComparatorMap();
  //private StateTable table = new StateTable();

  //private int currentState = 0;
  private Block currentBlock = new Block(null);

  //private Conditional currentConditional;

  private Table table = new Table(this, currentBlock);

  private int balancedEnd = 0;
  private Stack<MODE> previousMode = new Stack<MODE>();
  private MODE currentMode = HEADER;

  NFA(Proxy<T> proxy, T definitionUnderTest) {
    this.eventQueue = proxy.getEventQueue();
    this.proxyComponent =  proxy.getComponentCore();
    this.definitionUnderTest = definitionUnderTest;
    previousMode.push(HEADER);

    //repeat(1);
  }

  void addParticipant(Component c) {
    participants.add(c);
  }

  <P extends PortType> void addDisallowedEvent(
      KompicsEvent event, Port<P> port, Direction direction) {
    assertMode(HEADER);
    currentBlock.addDisallowedMessage(newEventSpec(event, port, direction));
  }

  <P extends  PortType> void addAllowedEvent(
      KompicsEvent event, Port<P> port, Direction direction) {
    assertMode(HEADER);
    currentBlock.addAllowedMessage(newEventSpec(event, port, direction));
  }

  <P extends  PortType> void addDroppedEvent(
      KompicsEvent event, Port<P> port, Direction direction) {
    assertMode(HEADER);
    currentBlock.addDroppedMessage(newEventSpec(event, port, direction));
  }

  <P extends PortType> void expectMessage(
      KompicsEvent event, Port<P> port, Direction direction) {
    EventSpec eventSpec = newEventSpec(event, port, direction);
    registerSpec(eventSpec);
  }

  <P extends PortType, E extends KompicsEvent> void expectMessage(
      Class<E> eventType, Predicate<E> predicate, Port<P> port, Direction direction) {
    PredicateSpec predicateSpec = new PredicateSpec(eventType, predicate, port, direction);
    registerSpec(predicateSpec);
  }

  <P extends PortType> void expectWithinBlock(
      KompicsEvent event, Port<P> port, Direction direction) {
    assertMode(HEADER);
    EventSpec eventSpec = newEventSpec(event, port, direction);
    currentBlock.expect(eventSpec);
  }

  <P extends PortType, E extends KompicsEvent> void expectWithinBlock(
      Class<E> eventType, Predicate<E> predicate, Port<P> port, Direction direction) {
    assertMode(HEADER);
    PredicateSpec predicateSpec = new PredicateSpec(eventType, predicate, port, direction);
    currentBlock.expect(predicateSpec);
  }

  void setUnorderedMode() {
    previousMode.push(currentMode);
    assertMode(BODY);
    setMode(UNORDERED);
    expectUnordered = new ArrayList<SingleEventSpec>();
    balancedEnd++;
/*    if (currentBlock.mode == CONDITIONAL) {
      currentBlock.previousMode = CONDITIONAL;
    } else {
      checkInBodyMode();
      currentBlock.previousMode = BODY;
    }
    currentBlock.mode = UNORDERED;
    expectUnordered = new ArrayList<SingleEventSpec>();*/
  }

  void setExpectWithMapperMode() {
    assertMode(BODY);
    previousMode.push(currentMode);
    setMode(EXPECT_MAPPER);
    balancedEnd++;
    expectMapper = new ExpectMapper(proxyComponent);
/*    if (currentBlock.mode == CONDITIONAL) {
      currentBlock.previousMode = CONDITIONAL;
    } else {
      checkInBodyMode();
      currentBlock.previousMode = BODY;
    }
    currentBlock.mode = EXPECT_MAPPER;
    expectMapper = new ExpectMapper(proxyComponent);*/
  }

  <E extends KompicsEvent, R extends KompicsEvent> void setMapperForNext(
      int expectedEvents, Class<E> eventType, Function<E, R> mapper) {
    assertMode(EXPECT_MAPPER);
    expectMapper.setMapperForNext(expectedEvents, eventType, mapper);
  }

  void addExpectWithMapper(
      Port<? extends PortType> listenPort, Port<? extends PortType> responsePort) {
    assertMode(EXPECT_MAPPER);
    expectMapper.addExpectedEvent(listenPort, responsePort);
  }

  <E extends KompicsEvent, R extends KompicsEvent> void addExpectWithMapper(
      Class<E> eventType, Port<? extends PortType> listenPort,
      Port<? extends PortType> responsePort, Function<E, R> mapper) {
    assertMode(EXPECT_MAPPER);
    expectMapper.addExpectedEvent(eventType, listenPort, responsePort, mapper);
  }

  void setExpectWithFutureMode() {
    assertMode(BODY);
    previousMode.push(currentMode);
    setMode(EXPECT_FUTURE);
    //// TODO: 4/25/17 move to decrement function
    balancedEnd++;
    expectFuture = new ExpectFuture(proxyComponent);
/*    if (currentBlock.mode == CONDITIONAL) {
      currentBlock.previousMode = CONDITIONAL;
    } else {
      checkInBodyMode();
      currentBlock.previousMode = BODY;
    }
    currentBlock.mode = EXPECT_FUTURE;
    expectFuture = new ExpectFuture(proxyComponent);*/
  }

  <E extends KompicsEvent, R extends KompicsEvent> void addExpectWithFuture(
      Class<E> eventType, Port<? extends PortType> listenPort, Future<E, R> future) {
    assertMode(EXPECT_FUTURE);
    expectFuture.addExpectedEvent(eventType, listenPort, future);
  }

  <E extends KompicsEvent, R extends KompicsEvent, P extends PortType> void trigger(
      Port<P> responsePort, Future<E, R> future) {
    assertMode(EXPECT_FUTURE);
    expectFuture.addTrigger(responsePort, future);
  }

  void trigger(KompicsEvent event, Port<? extends PortType> port) {
    InternalEventSpec spec = new InternalEventSpec(event, port);
    assertMode(BODY);
    table.addSpec(spec);
  }
  void trigger_(KompicsEvent event, Port<? extends PortType> port) {
/*    throw new UnsupportedOperationException("");
    InternalEventSpec spec = new InternalEventSpec(event, port);
    if (currentBlock.mode == CONDITIONAL) {
      currentConditional.addChild(spec);
    } else {
      checkInBodyMode();
      //table.addTransition(currentState, currentState + 1, spec, currentBlock);
      table.addSpec(spec);
      incrementState();
    }*/
  }

  void either() {
    throw new UnsupportedOperationException("");
/*    if (currentBlock.mode == BODY) {
      currentConditional = new Conditional(null);
      currentBlock.mode = CONDITIONAL;
    } else {
      checkMode(CONDITIONAL);
      Conditional child = new Conditional(currentConditional);
      currentConditional.addChild(child);
      currentConditional = child;
    }*/
  }

  void or() {
    throw new UnsupportedOperationException("");
/*    checkMode(CONDITIONAL);
    currentConditional.or();*/
  }

  private void endConditional() {
    throw new UnsupportedOperationException("");
/*    currentConditional.end();
    if (currentConditional.isMain()) {
      currentBlock.mode = BODY;
      currentState = currentConditional.resolve(currentState, table, currentBlock);
    } else {
      currentConditional = currentConditional.parent;
    }*/
  }

  void repeat(int count) {
/*    Block block = new Block(currentBlock, count, currentState);*/
/*    enterNewBlock(block);*/
    Block block = new Block(currentBlock);
    enterNewBlock(count, block);
  }

  void repeat(int count, BlockInit init) {
    Block block = new Block(currentBlock,  init);
    enterNewBlock(count, block);
  }

  void body() {
    assertMode(HEADER);
    setMode(BODY);
  }
/*  void body_() {
    checkInHeaderMode();
    currentBlock.mode = BODY;
  }*/

  void end() {
    switch (currentMode) {
      case UNORDERED:
        endUnorderedMode();
        break;
      case EXPECT_MAPPER:
        endExpect(currentMode);
        break;
      case EXPECT_FUTURE:
        endExpect(currentMode);
        break;
      case CONDITIONAL:
        endConditional();
        break;
      case BODY:
        endRepeat();
        break;
      default:
        throw new IllegalStateException("END not allowed in mode " + currentMode);
    }
  }

  void setIterationInit(BlockInit iterationInit) {
    assertMode(HEADER);
    currentBlock.setIterationInit(iterationInit);
  }

  // // TODO: 4/22/17 remove resolveAction if not needed later on
  void expectFault(
      Class<? extends Throwable> exceptionType, Fault.ResolveAction resolveAction) {
    assertMode(BODY);
    FaultSpec spec = new FaultSpec(definitionUnderTest.getControlPort(), exceptionType);
    table.addSpec(spec);
/*    checkInBodyMode();
    checkExpectedFaultHasMatchingClause();
    FaultSpec spec = new FaultSpec(definitionUnderTest.getControlPort(), exceptionType);
    table.addTransition(currentState, currentState + 1, spec, currentBlock);
    incrementState();*/
  }

  // // TODO: 4/22/17 remove resolveAction if not needed later on
  void expectFault(
      Predicate<Throwable> exceptionPredicate, Fault.ResolveAction resolveAction) {
    assertMode(BODY);
    FaultSpec spec = new FaultSpec(definitionUnderTest.getControlPort(), exceptionPredicate);
    table.addSpec(spec);
/*    checkInBodyMode();
    checkExpectedFaultHasMatchingClause();
    FaultSpec spec = new FaultSpec(definitionUnderTest.getControlPort(), exceptionPredicate);
    table.addTransition(currentState, currentState + 1, spec, currentBlock);
    incrementState();*/
  }

  void inspect(Predicate<T> inspectPredicate) {
    InternalEventSpec spec = new InternalEventSpec(definitionUnderTest, inspectPredicate);
    assertMode(BODY);
    table.addSpec(spec);
/*    InternalEventSpec spec = new InternalEventSpec(definitionUnderTest, inspectPredicate);
    if (currentBlock.mode == CONDITIONAL) {
      currentConditional.addChild(spec);
    } else {
      checkInBodyMode();
      table.addTransition(currentState, currentState + 1, spec, currentBlock);
      incrementState();
    }*/
  }

  <E extends KompicsEvent> void addComparator(
      Class<E> eventType, Comparator<E> comparator) {
    checkInInitialHeader();
    comparators.put(eventType, comparator);
  }

  <E extends KompicsEvent> void setDefaultAction(
      Class<E> eventType, Function<E, Action> function) {
    checkInInitialHeader();
    table.setDefaultAction(eventType, function);
  }

  public int getFinalState() {
    throw new UnsupportedOperationException("");
/*    return STARTED? FINAL_STATE : currentState;*/

  }

  void checkInInitialHeader() {
    assertMode(HEADER);
    if (currentBlock.previousBlock != null) {
      throw new IllegalStateException("Operation only supported in initial header");
    }
  }

  boolean start_() {
    if (!STARTED) {
      if (balancedEnd != 0) {
        throw new IllegalStateException("Unbalanced block");
      }
      STARTED = true;
      runStartState();
      return run();
    }
    return false;
  }
/*  int start() {
    if (!STARTED) {
      STARTED = true;
      addFinalState();
      checkBalancedRepeatBlocks();
      printTable(FINAL_STATE);
      run();
    }
    return currentState == FINAL_STATE + 1 ? FINAL_STATE : currentState;
  }*/

  <P extends  PortType, E extends KompicsEvent> EventSpec newEventSpec(
      KompicsEvent event, Port<P> port, Direction direction) {
    Comparator<E> c = (Comparator<E>) comparators.get(event.getClass());
    return EventSpec.create(c, (E) event, port, direction);
  }

  private boolean run() {
    table.build();
    while (true) {
      table.doInternalTransitions();
      if (table.isInFinalState()) {
        return true;
      }
      EventSpec receivedSpec = removeEventFromQueue();
      boolean successful = table.doTransition(receivedSpec);
      if (!successful) {
        return false;
      }
    }
  }

  private void registerSpec(SingleEventSpec spec) {
    switch (currentMode) {
      case BODY:
        table.addSpec(spec);
        break;
      case UNORDERED:
        expectUnordered.add(spec);
        break;
      default:
        fail(BODY);
    }
    //throw new UnsupportedOperationException("");
/*    if (currentBlock.mode == UNORDERED) {
      expectUnordered.add(spec);
    } else {
      checkInBodyMode();
      //table.addTransition(currentState, currentState + 1, spec, currentBlock);
      table.addSpec(spec);
      incrementState();
    }*/
  }

  private void endUnorderedMode() {
    if (expectUnordered.isEmpty()) {
      throw new IllegalStateException("No events were specified in unordered mode");
    }
    UnorderedSpec spec = new UnorderedSpec(expectUnordered);
    table.addSpec(spec);
    currentMode = previousMode.pop();
    balancedEnd--;

    /*

    UnorderedSpec spec = new UnorderedSpec(expectUnordered);
    if (currentBlock.previousMode == BODY) {
      table.addTransition(currentState, currentState + 1,
          spec, currentBlock);
      incrementState();
    } else {
      assert currentBlock.previousMode == CONDITIONAL;
      currentConditional.addChild(spec);
    }
    currentBlock.mode = currentBlock.previousMode;*/
  }

  private void endExpect(MODE mode) {
    Spec spec;
    boolean emptySpec;
    balancedEnd--;
    currentMode = previousMode.pop();
    if (mode == EXPECT_FUTURE) {
      spec = expectFuture;
      emptySpec = expectFuture.expected.isEmpty();
    } else if (mode == EXPECT_MAPPER) {
      spec = expectMapper;
      emptySpec = expectMapper.expected.isEmpty();
    } else {
      throw new IllegalStateException(String.format("Expected [%s] or [%s] mode",
          EXPECT_FUTURE, EXPECT_MAPPER));
    }
    if (emptySpec) {
      throw new IllegalStateException("No events were specified in " + mode + " mode");
    }
    table.addSpec(spec);
/*    Spec spec;
    boolean emptySpec;
    if (mode == EXPECT_FUTURE) {
      spec = expectFuture;
      emptySpec = expectFuture.expected.isEmpty();
    } else if (mode == EXPECT_MAPPER) {
      spec = expectMapper;
      emptySpec = expectMapper.expected.isEmpty();
    } else {
      throw new IllegalStateException(String.format("Expected [%s] or [%s] mode",
          EXPECT_FUTURE, EXPECT_MAPPER));
    }
    if (emptySpec) {
      throw new IllegalStateException("No events were specified in " + mode + " mode");
    }
    if (currentBlock.previousMode == CONDITIONAL) {
      currentConditional.addChild(spec);
    } else {
      table.addTransition(currentState, currentState + 1, spec, currentBlock);
      incrementState();
    }
    currentBlock.mode = currentBlock.previousMode;*/
  }

  private void endRepeat() {
    balancedEnd--;
    if (balancedEnd < 0) {
      throw new IllegalStateException("No matching block for end operation");
    }

    // block
    currentBlock = currentBlock.previousBlock;

    // mode
    currentMode = previousMode.pop();

    table.endRepeat();

/*    if (balancedBlock.isEmpty()) {
      throw new IllegalStateException("matching repeat not found for end");
    }*//*
    // // TODO: 4/23/17 move to method
    MODE previousMode = previousMode.pop();
    assertMode(previousMode, HEADER);

    balancedRepeat--;
    if (!balancedBlock.isEmpty()) {
      throw new IllegalStateException("");
    }
    // restore previous block
    currentBlock = currentBlock.previousBlock;*/
  }

  private void endBlock() {
    throw new UnsupportedOperationException("");
/*    checkInBodyMode();
    if (balancedBlock.isEmpty()) {
      throw new IllegalStateException("matching repeat not found for end");
    }

    blockEnd.put(currentState, currentBlock);
    // restore previous block
    if (!balancedBlock.isEmpty()) {
      currentBlock = balancedBlock.pop().previousBlock;
    }
    incrementState();*/
  }

  private void enterNewBlock(int count, Block block) {
    if (currentMode != BODY && currentMode != CONDITIONAL) {
      fail(BODY);
    }

    if (count <= 0) {
      throw new IllegalArgumentException("only positive value allowed for block");
    }

    // mode
    previousMode.push(currentMode);
    setMode(HEADER);

    // blocks
    balancedEnd++;

    // change block
    currentBlock = block;

    // table
    table.addRepeat(count, block);
  }

  private void enterNewBlock_(Block block) {
    throw new UnsupportedOperationException("");
/*    checkInBodyMode();
    if (block.times <= 0) {
      throw new IllegalArgumentException("only positive value allowed for block");
    }


    // mode
    setMode(HEADER);

    currentBlock = block;
    balancedBlock.push(currentBlock);
    blockStart.put(currentState, block);
    incrementState();*/
  }

  private boolean inMode(MODE mode) {
    return currentMode == mode;
  }

  private void assertMode(MODE mode) {
    if (currentMode != mode) {
      fail(mode);
    }
  }

  private void fail(MODE expected) {
    throw new IllegalStateException(String.format("Expected mode [%s], Actual mode [%s]",
          expected, currentMode));
  }

  private void setMode(MODE mode) {
    currentMode = mode;
    //previousMode.push(currentMode);
  }

/*  private void addFinalState() {
    FINAL_STATE = currentState;
    endBlock();
  }*/

/*  private void checkExpectedFaultHasMatchingClause() {
    //// TODO: 4/21/17 not necessarily previous if conditional
    int previousState = currentState - 1;
    if (!(table.hasState(previousState))) {
      throw new IllegalStateException("expected fault must be preceded by an expect or trigger");
    }
  }*/

/*  private boolean expectingAnEvent() {
    return !(isStartOfBlock() || isEndOfBlock()
        || performedInternalTransition());
  }*/

  // returns false if state was updated to error state
/*
  private boolean updateState(
      String expected, EventSpec receivedSpec, StateTable.Transition transition) {
    if (transition != null && transition.nextState != ERROR_STATE) {
      logger.debug("{}: Transitioned on {}", currentState, transition);
      currentState = transition.nextState;
      return true;
    }
    String errorMessage = String.format("Received %s message <%s> while expecting <%s>",
        (transition == null? "unexpected" : "unwanted"),
        receivedSpec.toString(), expected);
    gotoErrorState(errorMessage);
    return false;
  }
*/

/*
  private boolean performedInternalTransition() {
    StateTable.Transition transition = table.performInternalTransition(currentState, false);
    if (transition == null) {
      return false;
    }
    if (transition.errorMessage != null) {
      gotoErrorState(transition.errorMessage);
    } else {
      currentState = transition.nextState;
    }
    return true;
  }

  private boolean isStartOfBlock() {
    Block block = blockStart.get(currentState);
    if (block == null) {
      return false;
    } else {
      block.initialize();
      logger.debug("{}: repeat({})\t", currentState, block.getCurrentCount());
      incrementState();
      return true;
    }
  }

  private boolean isEndOfBlock() {
    Block block = blockEnd.get(currentState);
    if (block == null) { // not end of block
      return false;
    }
    logger.debug("{}: end({})\t", currentState, block.times);

    while (block.hasPendingEvents()) {
      logger.debug("{}: Awaiting events {}", currentState, block.status());
      if (!handleNextEvent(block.status(), block)) {
        return true;
      }
    }
    block.iterationComplete();

    if (block.hasMoreIterations()) {
      currentState = block.indexOfFirstState();
    } else {
      incrementState();
    }
    return true;
  }

  private boolean handleNextEvent(String expected, Block block) {
    EventSpec received = removeEventFromQueue();
    StateTable.Transition transition;

    // if no event received -
    // try perform an internal event transition in-case current state -
    // is a merged with a normal spec
    // if not, try an e-transition for state
    if (received == null) {
      transition = table.performInternalTransition(currentState, true);
      if (transition != null) {
        if (transition.errorMessage != null) {
          gotoErrorState(transition.errorMessage);
          return false;
        } else {
          currentState = transition.nextState;
          return true;
        }
      }
      received = EventSpec.EPSILON;
    }
    logger.debug("{}: Received {}", currentState, received);

    //// TODO: 4/21/17 separate block use case
    // use block for lookup if given
    if (block == null) {
      transition = table.lookup(currentState, received);
    } else {
      transition = table.lookup(currentState, received, block);
    }

    // if transition not found for normal event, try any e-transitions before failing
    if (received != EventSpec.EPSILON && block == null) {
      // take e-transition to next state and lookup event
      while (transition == null) {
        transition = table.lookup(currentState, EventSpec.EPSILON);
        if (transition != null) {
          currentState = transition.nextState;
          transition = table.lookup(currentState, received);
        } else {
          break;
        }
      }
    }

    logger.debug("Transition = {}", transition);
    if (transition == null && received == EventSpec.EPSILON) {
      gotoErrorState("timed-out expecting " + expected);
      return false;
    }
    return updateState(expected, received, transition);
  }
*/

  private void runStartState() {
    logger.trace("Sending Start to {} participant component(s)", participants.size());
    for (Component child : participants) {
      child.getControl().doTrigger(Start.event, 0, proxyComponent);
    }
  }

/*
  private void runFinalState() {
    logger.info("Done!({})", currentState == ERROR_STATE? "FAILURE -> " + ERROR_MESSAGE : "PASS");
    Kompics.shutdown();
  }

  private void checkBalancedRepeatBlocks() {
    if (!balancedBlock.isEmpty()) {
      throw new IllegalStateException("unmatched end for block");
    }
  }

  private void checkMode(MODE mode) {
    if (currentBlock != null && currentBlock.mode != mode) {
      throw new IllegalStateException(String.format("Expected mode [%s], Actual mode [%s]",
          mode, currentBlock.mode));
    }
  }

  private void checkInBodyMode() {
    if (currentBlock == null) {
      return;
    }
    checkMode(BODY);
  }

  private void checkInHeaderMode() {
    checkMode(HEADER);
  }

  private void checkInExpectMapperMode() {
    checkMode(EXPECT_MAPPER);
  }

  private void checkInExpectFutureMode() {
    checkMode(EXPECT_FUTURE);
  }

  private void incrementState() {
    currentState++;
  }

  private void gotoErrorState(String errorMessage) {
    currentState = ERROR_STATE;
    ERROR_MESSAGE = errorMessage;
  }
*/

  private EventSpec removeEventFromQueue() {
    return eventQueue.poll();
  }
/*
  private void printTable(int final_state) {
    Testkit.logger.info("State\t\t\t\tTransitions");
    for (int i = 0; i <= final_state; i++) {
      StateTable.State state = table.states.get(i);
      if (state != null) { // expecting state
        logger.info("{}", i);
        logger.info("\t\t {}", state);
      } else if (blockStart.containsKey(i)) { // start of block
        logger.info("{}\t\t{}",i, blockStart.get(i));
      } else if (blockEnd.containsKey(i)) { // end of block
        logger.info("{}\t\tend{}",i, blockEnd.get(i));
      }
    }

  }*/

  private class ComparatorMap {
    Map<Class<? extends KompicsEvent>, Comparator<? extends KompicsEvent>> comparators =
        new HashMap<Class<? extends KompicsEvent>, Comparator<? extends KompicsEvent>>();

    @SuppressWarnings("unchecked")
    public <E extends KompicsEvent> Comparator<E> get(Class<E> eventType) {
      return (Comparator<E>) comparators.get(eventType);
    }

    public <E extends KompicsEvent> void put(Class<E> eventType, Comparator<E> comparator) {
      comparators.put(eventType, comparator);
    }
  }
}
