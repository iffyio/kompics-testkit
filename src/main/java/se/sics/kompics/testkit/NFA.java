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
import static se.sics.kompics.testkit.Block.STAR;

class NFA<T extends ComponentDefinition> {
  static final Logger logger = Testkit.logger;

  private final T definitionUnderTest;
  private final EventQueue eventQueue;

  private boolean STARTED = false;

  private final ComponentCore proxyComponent;
  private Collection<Component> participants = new HashSet<Component>();

  private ExpectMapper expectMapper;
  private ExpectFuture expectFuture;
  private List<SingleEventSpec> expectUnordered = new ArrayList<SingleEventSpec>();
  private ComparatorMap comparators = new ComparatorMap();

  private Block currentBlock = new Block();

  private Table table = new Table(currentBlock);

  private int balancedEnd = 0;
  private Stack<MODE> previousMode = new Stack<MODE>();
  private MODE currentMode = HEADER;

  NFA(Proxy<T> proxy, T definitionUnderTest) {
    this.eventQueue = proxy.getEventQueue();
    this.proxyComponent =  proxy.getComponentCore();
    this.definitionUnderTest = definitionUnderTest;
    previousMode.push(HEADER);
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
/*    previousMode.push(currentMode);
    assertMode(BODY);
    setMode(UNORDERED);*/
    assertBodyorConditionalMode();
    pushNewMode(UNORDERED);
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
/*    assertMode(BODY);
    previousMode.push(currentMode);
    setMode(EXPECT_MAPPER);*/
    assertBodyorConditionalMode();
    pushNewMode(EXPECT_MAPPER);
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
/*    assertMode(BODY);
    previousMode.push(currentMode);
    setMode(EXPECT_FUTURE);*/
    assertBodyorConditionalMode();
    pushNewMode(EXPECT_FUTURE);
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
    assertBodyorConditionalMode();
    table.addSpec(spec);
  }

  void either() {
    assertBodyorConditionalMode();
    pushNewMode(CONDITIONAL);
    table.either(currentBlock);
    balancedEnd++;
  }

  void or() {
    assertBodyorConditionalMode();
    table.or();
  }

  private void endConditional() {
    assertMode(CONDITIONAL);
    currentMode = previousMode.pop();
    balancedEnd--;
    table.endRepeat();
  }

  void repeat(int count) {
/*    Block block = new Block(currentBlock, count, currentState);*/
/*    enterNewBlock(block);*/
    Block block = new Block(currentBlock, count);
    enterNewBlock(count, block);
  }

  void repeat() {
/*    Block block = new Block(currentBlock, count, currentState);*/
/*    enterNewBlock(block);*/
    Block block = new Block(currentBlock, STAR);
    enterNewBlock(STAR, block);
  }

  void repeat(int count, BlockInit init) {
    Block block = new Block(currentBlock, count, init);
    enterNewBlock(count, block);
  }

  void repeat(BlockInit init) {
    Block block = new Block(currentBlock, STAR, init);
    enterNewBlock(STAR, block);
  }

  void body() {
    assertMode(HEADER);
    setMode(BODY);
  }

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
    assertBodyorConditionalMode();
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
    assertBodyorConditionalMode();
    FaultSpec spec = new FaultSpec(definitionUnderTest.getControlPort(), exceptionPredicate);
    table.addSpec(spec);
/*    checkInBodyMode();
    checkExpectedFaultHasMatchingClause();
    FaultSpec spec = new FaultSpec(definitionUnderTest.getControlPort(), exceptionPredicate);
    table.addTransition(currentState, currentState + 1, spec, currentBlock);
    incrementState();*/
  }

  void inspect(Predicate<T> inspectPredicate) {
    assertBodyorConditionalMode();
    InternalEventSpec spec = new InternalEventSpec(definitionUnderTest, inspectPredicate);
    table.addSpec(spec);
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

  <P extends  PortType, E extends KompicsEvent> EventSpec newEventSpec(
      KompicsEvent event, Port<P> port, Direction direction) {
    Comparator<E> c = (Comparator<E>) comparators.get(event.getClass());
    return EventSpec.create(c, (E) event, port, direction);
  }

  private boolean run() {
    table.build();
    while (true) {
      table.tryInternalEventTransitions();
      EventSpec receivedSpec = removeEventFromQueue();
      if (receivedSpec == null && table.isInFinalState()) {
        logger.debug("final state");
        return true;
      }
      boolean successful = table.doTransition(receivedSpec);
/*      if (table.isInFinalState()) {
        logger.debug("final state");
        return true;
      }*/
      if (!successful && !table.isInFinalState()) {
        return false;
      }
    }
  }

  private void registerSpec(SingleEventSpec spec) {
    switch (currentMode) {
      case BODY:
      case CONDITIONAL:
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
  }

  private void enterNewBlock(int count, Block block) {
    assertBodyorConditionalMode();
    pushNewMode(HEADER);

    if (count <= 0 && count != Block.STAR) {
      throw new IllegalArgumentException("only positive value allowed for block");
    }

    // mode
/*    previousMode.push(currentMode);
    setMode(HEADER);*/

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

  private void pushNewMode(MODE mode) {
    previousMode.push(currentMode);
    setMode(mode);
  }

  private void assertBodyorConditionalMode() {
    if (!(currentMode == BODY || currentMode == CONDITIONAL)) {
      throw new IllegalStateException(
          String.format("Expected mode [%s] or [%s], Current mode [%s]",
              BODY, CONDITIONAL, currentMode));
    }
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


  private void runStartState() {
    logger.trace("Sending Start to {} participant component(s)", participants.size());
    for (Component child : participants) {
      child.getControl().doTrigger(Start.event, 0, proxyComponent);
    }
  }


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
