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
import se.sics.kompics.Kompics;
import se.sics.kompics.PortType;
import se.sics.kompics.Port;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Start;

import static se.sics.kompics.testkit.Block.MODE;
import static se.sics.kompics.testkit.Block.MODE.*;

class FSM<T extends ComponentDefinition> {
  static final Logger logger = Testkit.logger;

  private final T definitionUnderTest;
  private final EventQueue eventQueue;

  static final int ERROR_STATE = -1;
  private String ERROR_MESSAGE = "";
  private int FINAL_STATE;
  private boolean STARTED = false;

  private final ComponentCore proxyComponent;
  private Collection<Component> participants = new HashSet<Component>();

  private final Stack<Block> balancedBlock = new Stack<Block>();
  private Map<Integer, Block> blockStart = new HashMap<Integer, Block>();
  private Map<Integer, Block> blockEnd = new HashMap<Integer, Block>();

  private List<SingleEventSpec> expectUnordered = new ArrayList<SingleEventSpec>();
  private ExpectMapper expectMapper;
  private ExpectFuture expectFuture;

  private Map<Integer, ExpectedFault> expectedFaults = new HashMap<Integer, ExpectedFault>();

  private ComparatorMap comparators = new ComparatorMap();
  private StateTable table = new StateTable();

  private Block currentBlock;
  private int currentState = 0;

  private Conditional currentConditional;

  FSM(Proxy<T> proxy, T definitionUnderTest) {
    this.eventQueue = proxy.getEventQueue();
    this.proxyComponent =  proxy.getComponentCore();
    this.definitionUnderTest = definitionUnderTest;

    repeat(1);
  }

  void addParticipant(Component c) {
    participants.add(c);
  }

  <P extends PortType> void addDisallowedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    checkInHeaderMode();
    currentBlock.addDisallowedMessage(newEventSpec(event, port, direction));
  }

  <P extends  PortType> void addAllowedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    checkInHeaderMode();
    currentBlock.addAllowedMessage(newEventSpec(event, port, direction));
  }

  <P extends  PortType> void addDroppedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    checkInHeaderMode();
    currentBlock.addDroppedMessage(newEventSpec(event, port, direction));
  }

  <P extends PortType> void expectMessage(
          KompicsEvent event, Port<P> port, Direction direction) {
    EventSpec eventSpec = newEventSpec(event, port, direction);
    if (currentBlock.mode == CONDITIONAL) {
      currentConditional.addChild(eventSpec);
    } else {
      registerSpec(eventSpec);
    }
  }

  <P extends PortType, E extends KompicsEvent> void expectMessage(
          Class<E> eventType, Predicate<E> predicate, Port<P> port, Direction direction) {
    PredicateSpec predicateSpec = new PredicateSpec(eventType, predicate, port, direction);
    registerSpec(predicateSpec);
  }

  <P extends PortType> void expectWithinBlock(
          KompicsEvent event, Port<P> port, Direction direction) {
    EventSpec eventSpec = newEventSpec(event, port, direction);
    checkInHeaderMode();
    currentBlock.expectWithinBlock(eventSpec);
  }

  <P extends PortType, E extends KompicsEvent> void expectWithinBlock(
          Class<E> eventType, Predicate<E> predicate, Port<P> port, Direction direction) {
    PredicateSpec predicateSpec = new PredicateSpec(eventType, predicate, port, direction);
    checkInHeaderMode();
    currentBlock.expectWithinBlock(predicateSpec);
  }

  void setUnorderedMode() {
    if (currentBlock.mode == CONDITIONAL) {
      currentBlock.previousMode = CONDITIONAL;
    } else {
      checkInBodyMode();
      currentBlock.previousMode = BODY;
    }
    currentBlock.mode = UNORDERED;
    expectUnordered = new ArrayList<SingleEventSpec>();
  }

  void setExpectWithMapperMode() {
    if (currentBlock.mode == CONDITIONAL) {
      currentBlock.previousMode = CONDITIONAL;
    } else {
      checkInBodyMode();
      currentBlock.previousMode = BODY;
    }
    currentBlock.mode = EXPECT_MAPPER;
    expectMapper = new ExpectMapper(proxyComponent);
  }

  <E extends KompicsEvent, R extends KompicsEvent> void setMapperForNext(
          int expectedEvents, Class<E> eventType, Function<E, R> mapper) {
    checkInExpectMapperMode();
    expectMapper.setMapperForNext(expectedEvents, eventType, mapper);
  }

  void addExpectWithMapper(
          Port<? extends PortType> listenPort, Port<? extends PortType> responsePort) {
    checkInExpectMapperMode();
    expectMapper.addExpectedEvent(listenPort, responsePort);
  }

  <E extends KompicsEvent, R extends KompicsEvent> void addExpectWithMapper(
          Class<E> eventType, Port<? extends PortType> listenPort,
          Port<? extends PortType> responsePort, Function<E, R> mapper) {
    checkInExpectMapperMode();
    expectMapper.addExpectedEvent(eventType, listenPort, responsePort, mapper);
  }

  void setExpectWithFutureMode() {
    if (currentBlock.mode == CONDITIONAL) {
      currentBlock.previousMode = CONDITIONAL;
    } else {
      checkInBodyMode();
      currentBlock.previousMode = BODY;
    }
    currentBlock.mode = EXPECT_FUTURE;
    expectFuture = new ExpectFuture(proxyComponent);
  }

  <E extends KompicsEvent, R extends KompicsEvent> void addExpectWithFuture(
          Class<E> eventType, Port<? extends PortType> listenPort, Future<E, R> future) {
    checkInExpectFutureMode();
    expectFuture.addExpectedEvent(eventType, listenPort, future);
  }

  <E extends KompicsEvent, R extends KompicsEvent, P extends PortType> void trigger(
          Port<P> responsePort, Future<E, R> future) {
    checkInExpectFutureMode();
    expectFuture.addTrigger(responsePort, future);
  }

  void trigger(KompicsEvent event, Port<? extends PortType> port) {
    InternalEventSpec spec = new InternalEventSpec(event, port);
    if (currentBlock.mode == CONDITIONAL) {
      currentConditional.addChild(spec);
    } else {
      checkInBodyMode();
      table.addTransition(currentState, currentState + 1, spec, currentBlock);
      incrementState();
    }
  }

  void either() {
    if (currentBlock.mode == BODY) {
      currentConditional = new Conditional(null);
      currentBlock.mode = CONDITIONAL;
    } else {
      checkMode(CONDITIONAL);
      Conditional child = new Conditional(currentConditional);
      currentConditional.addChild(child);
      currentConditional = child;
    }
  }

  void or() {
    checkMode(CONDITIONAL);
    currentConditional.or();
  }

  private void endConditional() {
    currentConditional.end();
    if (currentConditional.isMain()) {
      currentBlock.mode = BODY;
      currentState = currentConditional.resolve(currentState, table, currentBlock);
    } else {
      currentConditional = currentConditional.parent;
    }
  }

  void repeat(int times) {
    Block block = new Block(currentBlock, times, currentState);
    enterNewBlock(block);
  }

  void repeat(int times, BlockInit init) {
    Block block = new Block(currentBlock, times, currentState, init);
    enterNewBlock(block);
  }

  void body() {
    checkInHeaderMode();
    currentBlock.mode = BODY;
  }

  void end() {
    MODE mode = currentBlock.mode;
    switch (mode) {
      case UNORDERED:
        endUnorderedMode();
        break;
      case EXPECT_MAPPER:
        endExpect(mode);
        break;
      case EXPECT_FUTURE:
        endExpect(mode);
        break;
      case CONDITIONAL:
        endConditional();
        break;
      default:
        endBlock();
    }
  }

  void setIterationInit(BlockInit iterationInit) {
    checkInHeaderMode();
    currentBlock.setIterationInit(iterationInit);
  }

  void expectFault(
          Class<? extends Throwable> exceptionType, Fault.ResolveAction resolveAction) {
    checkInBodyMode();
    checkExpectedFaultHasMatchingClause();
    expectedFaults.put(currentState, new ExpectedFault(exceptionType, resolveAction));
    incrementState();
  }

  void expectFault(
          Predicate<Throwable> exceptionPredicate, Fault.ResolveAction resolveAction) {
    checkInBodyMode();
    checkExpectedFaultHasMatchingClause();
    expectedFaults.put(currentState, new ExpectedFault(exceptionPredicate, resolveAction));
    incrementState();
  }

  void inspect(Predicate<T> inspectPredicate) {
    InternalEventSpec spec = new InternalEventSpec(definitionUnderTest, inspectPredicate);
    if (currentBlock.mode == CONDITIONAL) {
      currentConditional.addChild(spec);
    } else {
      checkInBodyMode();
      table.addTransition(currentState, currentState + 1, spec, currentBlock);
      incrementState();
    }
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
    return STARTED? FINAL_STATE : currentState;
  }

  void checkInInitialHeader() {
    checkInHeaderMode();
    if (currentBlock == null || currentBlock.previousBlock != null) {
      throw new IllegalStateException("Operation only supported in initial header");
    }
  }

  int start() {
    if (!STARTED) {
      STARTED = true;
      addFinalState();
      checkBalancedRepeatBlocks();
      printTable(FINAL_STATE);
      run();
    }
    return currentState == FINAL_STATE + 1 ? FINAL_STATE : currentState;
  }

  <P extends  PortType, E extends KompicsEvent> EventSpec newEventSpec(
          KompicsEvent event, Port<P> port, Direction direction) {
    Comparator<E> c = (Comparator<E>) comparators.get(event.getClass());
    return EventSpec.create(c, (E) event, port, direction);
  }

  ExpectedFault getExpectedFault() {
    // // TODO: 3/21/17 use view with (expect, fault) pair
    int initialState = currentState;
    ExpectedFault expectedFault = expectedFaults.get(initialState);
    if (expectedFault == null) { // try next state
      expectedFault = expectedFaults.get(initialState + 1);
    }
    return expectedFault;
  }

  private void run() {
    runStartState();
    currentState = 0;
    while (currentState <= FINAL_STATE && currentState != ERROR_STATE) {
      if (expectingAnEvent()) {
        table.printExpectedEventAt(currentState);
        String expected = table.getExpectedSpecAt(currentState);
        handleNextEvent(expected, null);
      }
    }
    runFinalState();
  }

  // // TODO: 3/31/17 only allow in body, unordered mode
  private void registerSpec(SingleEventSpec spec) {
    if (currentBlock.mode == UNORDERED) {
      expectUnordered.add(spec);
    } else {
      checkInBodyMode();
      table.addTransition(currentState, currentState + 1, spec, currentBlock);
      incrementState();
    }
  }

  private void endUnorderedMode() {
    if (expectUnordered.isEmpty()) {
      throw new IllegalStateException("No events were specified in unordered mode");
    }

    UnorderedSpec spec = new UnorderedSpec(expectUnordered);
    if (currentBlock.previousMode == BODY) {
      table.addTransition(currentState, currentState + 1,
          spec, currentBlock);
      incrementState();
    } else {
      assert currentBlock.previousMode == CONDITIONAL;
      currentConditional.addChild(spec);
    }
    currentBlock.mode = currentBlock.previousMode;
  }

  private void endExpect(MODE mode) {
    Spec spec;
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
    currentBlock.mode = currentBlock.previousMode;
  }

  private void endBlock() {
    checkInBodyMode();
    if (balancedBlock.isEmpty()) {
      throw new IllegalStateException("matching repeat not found for end");
    }

    blockEnd.put(currentState, currentBlock);
    // restore previous block
    if (!balancedBlock.isEmpty()) {
      currentBlock = balancedBlock.pop().previousBlock;
    }
    incrementState();
  }

  private void enterNewBlock(Block block) {
    checkInBodyMode();
    if (block.times <= 0) {
      throw new IllegalArgumentException("only positive value allowed for block");
    }

    currentBlock = block;
    balancedBlock.push(currentBlock);
    blockStart.put(currentState, block);
    incrementState();
  }

  private void addFinalState() {
    FINAL_STATE = currentState;
    endBlock();
  }

  private void checkExpectedFaultHasMatchingClause() {
    //// TODO: 4/21/17 not necessarily previous if conditional
    int previousState = currentState - 1;
    if (!(table.hasState(previousState))) {
      throw new IllegalStateException("expected fault must be preceded by an expect or trigger");
    }
  }

  private boolean expectingAnEvent() {
    return !(isStartOfBlock() || isEndOfBlock()
            || performedInternalTransition() || expectedFault());
  }

  // returns false if state was updated to error state
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

  private boolean expectedFault() {
    ExpectedFault expectedFault = this.expectedFaults.get(currentState);
    if (expectedFault == null) {
      return false;
    }
    logger.info("Expect fault matching {}", expectedFault.strReprOfExpectedException());

    ExpectedFault.Result result = expectedFault.getResult();
    String assertMessage = result.message;

    if (result.succeeded) {
      logger.debug(assertMessage);
      incrementState();
    } else {
      gotoErrorState(assertMessage);
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

  private void runStartState() {
    logger.trace("Sending Start to {} participant component(s)", participants.size());
    for (Component child : participants) {
      child.getControl().doTrigger(Start.event, 0, proxyComponent);
    }
  }

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

  private EventSpec removeEventFromQueue() {
    return eventQueue.poll();
  }

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

  }
  // // TODO: 2/17/17 switch to eventSpec?
  private class Trigger {
    private final KompicsEvent event;
    private final Port<? extends PortType> port;

    Trigger(KompicsEvent event, Port<? extends PortType> port) {
      this.event = event;
      this.port = port;
    }

    void doTrigger() {
      port.doTrigger(event, 0, port.getOwner());
    }

    @Override
    public String toString() {
      return event.toString();
    }
  }

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
