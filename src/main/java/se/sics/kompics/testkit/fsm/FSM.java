package se.sics.kompics.testkit.fsm;

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
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Kompics;
import se.sics.kompics.PortType;
import se.sics.kompics.Port;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Start;

import se.sics.kompics.testkit.Action;
import se.sics.kompics.testkit.BlockInit;
import se.sics.kompics.testkit.ExpectedFault;
import se.sics.kompics.testkit.Direction;
import se.sics.kompics.testkit.Proxy;

public class FSM<T extends ComponentDefinition> {
  static final Logger logger = LoggerFactory.getLogger("Testkit");

  private final T definitionUnderTest;
  public static final int ERROR_STATE = -1;
  private int FINAL_STATE;
  private boolean STARTED = false;
  private String ERROR_MESSAGE = "";

  private final ComponentCore proxyComponent;
  private Collection<Component> participants = new HashSet<Component>();
  private final Stack<Block> balancedBlock = new Stack<Block>();
  private List<Spec> expectUnordered = new ArrayList<>();
  private final EventQueue eventQueue;

  private Map<Integer, Block> blockStart = new HashMap<Integer, Block>();
  private Map<Integer, Block> blockEnd = new HashMap<Integer, Block>();
  private Map<Integer, Trigger> triggeredEvents = new HashMap<Integer, Trigger>();
  private Map<Integer, Predicate<T>> componentPredicates = new HashMap<Integer, Predicate<T>>();
  private Map<Integer, ExpectedFault> expectedFaults = new HashMap<Integer, ExpectedFault>();

  private ComparatorMap comparators = new ComparatorMap();
  private StateTable table = new StateTable();

  private Block currentBlock;
  private int currentState = 0;

  public FSM(Proxy<T> proxy, T definition) {
    this.eventQueue = proxy.getEventQueue();
    this.proxyComponent =  proxy.getComponentCore();
    definitionUnderTest = definition;

    initializeFSM();
  }

  private void initializeFSM() {
    repeat(1);
  }

  public void addParticipatingComponents(Component c) {
    participants.add(c);
  }

  public <P extends PortType> void addDisallowedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    checkInHeaderMode();
    currentBlock.addDisallowedMessage(newEventSpec(event, port, direction));
  }

  public <P extends  PortType> void addAllowedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    checkInHeaderMode();
    currentBlock.addAllowedMessage(newEventSpec(event, port, direction));
  }

  public <P extends  PortType> void addDroppedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    checkInHeaderMode();
    currentBlock.addDroppedMessage(newEventSpec(event, port, direction));
  }

  public <P extends PortType> void expectMessage(
          KompicsEvent event, Port<P> port, Direction direction) {
    EventSpec<? extends KompicsEvent> eventSpec = newEventSpec(event, port, direction);
    registerSpec(eventSpec);
  }

  public <P extends PortType, E extends KompicsEvent> void expectMessage(
          Class<E> eventType, Predicate<E> pred, Port<P> port, Direction direction) {
    PredicateSpec predicateSpec = new PredicateSpec(eventType, pred, port, direction);
    registerSpec(predicateSpec);
  }

  public <P extends PortType> void expectWithinBlock(
          KompicsEvent event, Port<P> port, Direction direction) {
    EventSpec<? extends KompicsEvent> eventSpec = newEventSpec(event, port, direction);
    checkInHeaderMode();
    currentBlock.expectWithinBlock(eventSpec);
  }

  public <P extends PortType, E extends KompicsEvent> void expectWithinBlock(
          Class<E> eventType, Predicate<E> pred, Port<P> port, Direction direction) {
    PredicateSpec predicateSpec = new PredicateSpec(eventType, pred, port, direction);
    checkInHeaderMode();
    currentBlock.expectWithinBlock(predicateSpec);
  }

  private void registerSpec(Spec spec) {
    if (inUnorderedMode()) {
      expectUnordered.add(spec);
    } else {
      checkInBodyMode();
      table.registerExpectedEvent(currentState, spec, currentBlock);
      currentState++;
    }
  }

  public void setUnorderedMode() {
    checkInBodyAndNotUnorderedMode();
    currentBlock.mode = Block.MODE.UNORDERED;
    expectUnordered = new ArrayList<Spec>();
  }

  private void endUnorderedMode() {
    currentBlock.mode = Block.MODE.BODY;
    if (expectUnordered.isEmpty()) {
      throw new IllegalStateException("No events were specified in unordered mode");
    }

    table.registerExpectedEvent(currentState, expectUnordered, currentBlock);
    currentState++;
  }

  public void addTrigger(KompicsEvent event, Port<? extends PortType> port) {
    checkInBodyAndNotUnorderedMode();
    triggeredEvents.put(currentState, new Trigger(event, port));
    currentState++;
  }

  public void repeat(int times) {
    Block block = new Block(currentBlock, times, currentState);
    enterNewBlock(block);
  }

  public void repeat(int times, BlockInit init) {
    Block block = new Block(currentBlock, times, currentState, init);
    enterNewBlock(block);
  }

  public void body() {
    checkInHeaderMode();
    currentBlock.mode = Block.MODE.BODY;
  }

  public void end() {
    if (inUnorderedMode()) {
      endUnorderedMode();
    } else {
      endBlock();
    }
  }

  private void endBlock() {
    checkInBodyAndNotUnorderedMode();
    if (balancedBlock.isEmpty()) {
      throw new IllegalStateException("matching repeat not found for end");
    }
    blockEnd.put(currentState, currentBlock);
    restorePreviousBlock();
    currentState++;
  }

  public void setIterationInit(BlockInit iterationInit) {
    checkInHeaderMode();
    currentBlock.setIterationInit(iterationInit);
  }

  public void addExpectedFault(
          Class<? extends Throwable> exceptionType, Fault.ResolveAction resolveAction) {
    checkInBodyAndNotUnorderedMode();
    checkExpectedFaultHasMatchingClause();
    expectedFaults.put(currentState, new ExpectedFault(exceptionType, resolveAction));
    currentState++;
  }

  public void addExpectedFault(
          Predicate<Throwable> exceptionPredicate, Fault.ResolveAction resolveAction) {
    checkInBodyAndNotUnorderedMode();
    checkExpectedFaultHasMatchingClause();
    expectedFaults.put(currentState, new ExpectedFault(exceptionPredicate, resolveAction));
    currentState++;
  }

  public void addAssertComponent(Predicate<T> assertPred) {
    componentPredicates.put(currentState, assertPred);
    currentState++;
  }

  public <E extends KompicsEvent> void addComparator(
          Class<E> eventType, Comparator<E> comparator) {
    checkInInitialHeader();
    comparators.put(eventType, comparator);
  }

  public <E extends KompicsEvent> void setDefaultAction(
          Class<E> eventType, Function<E, Action> function) {
    checkInInitialHeader();
    table.setDefaultAction(eventType, function);
  }

  public int getFinalState() {
    return STARTED? FINAL_STATE : currentState;
  }

  public void checkInInitialHeader() {
    checkInHeaderMode();
    if (currentBlock == null || currentBlock.previousBlock != null) {
      throw new IllegalStateException("Operation only supported in initial header");
    }
  }

  public int start() {
    if (!STARTED) {
      STARTED = true;
      addFinalState();
      checkBalancedRepeatBlocks();
      table.printTable(FINAL_STATE);
      run();
    }
    return currentState == FINAL_STATE + 1 ? FINAL_STATE : currentState;
  }

  // PACKAGE_PRIVATE

  public ExpectedFault getExpectedFault() {
    // // TODO: 3/21/17 use view with (expect, fault) pair
    int initialState = currentState;
    ExpectedFault expectedFault = expectedFaults.get(initialState);
    if (expectedFault == null) { // try next state
      expectedFault = expectedFaults.get(initialState + 1);
    }
    return expectedFault;
  }

  // PRIVATE

  private void run() {
    runStartState();
    currentState = 0;
    while (currentState <= FINAL_STATE && currentState != ERROR_STATE) {
      if (expectingAnEvent()) {
        table.printExpectedEventAt(currentState);
        String expected = table.getExpectedSpecAt(currentState);

        EventSpec<? extends KompicsEvent> received = removeEventFromQueue();
        setComparatorForEvent(received);

        StateTable.Transition transition = table.lookup(currentState, received);

        if (!transitionedToErrorState(expected, received.toString(), transition)) {
          logger.debug("{}: Matched({}) with Transition({})", currentState, received, transition);
          currentState = transition.nextState;
        }
      }
    }
    runFinalState();
  }

  public <P extends  PortType, E extends KompicsEvent> EventSpec<? extends KompicsEvent> newEventSpec(
          KompicsEvent event, Port<P> port, Direction direction) {
    Comparator<E> c = (Comparator<E>) comparators.get(event.getClass());
    return EventSpec.create(c, (E) event, port, direction);
  }

  private void enterNewBlock(Block block) {
    checkInBodyAndNotUnorderedMode();
    if (block.times <= 0) {
      throw new IllegalArgumentException("only positive value allowed for block");
    }

    currentBlock = block;
    balancedBlock.push(currentBlock);
    blockStart.put(currentState, block);
    currentState++;
  }

  private void restorePreviousBlock() {
    if (!balancedBlock.isEmpty()) {
      currentBlock = balancedBlock.pop().previousBlock;
    }
  }

  private void addFinalState() {
    FINAL_STATE = currentState;
    endBlock();
  }

  private void checkExpectedFaultHasMatchingClause() {
    int previousState = currentState - 1;
    if (!(table.isExpectState(previousState) || triggeredEvents.containsKey(previousState))) {
      throw new IllegalStateException("expected fault must be preceded by an expect or trigger");
    }
  }

  private boolean expectingAnEvent() {
    return !(isStartOfBlock() || isEndOfBlock() || triggeredAnEvent()
            || assertedComponent() || expectedFault());
  }

  private boolean transitionedToErrorState(
          String expected, String received, StateTable.Transition transition) {
    if (transition != null && transition.nextState != ERROR_STATE) {
      return false;
    }
    String errorMessage = String.format("Received %s message <%s> while expecting <%s>",
                            (transition == null? "unexpected" : "unwanted"), received, expected);
    gotoErrorState(errorMessage);
    return true;
  }

  private boolean assertedComponent() {
    Predicate<T> assertPred = componentPredicates.get(currentState);
    if (assertPred == null) {
      return false;
    }
    logger.debug("{}: Asserting Component", currentState);
    boolean successful = assertPred.apply(definitionUnderTest);

    if (!successful) {
      gotoErrorState("Component assertion failed");
    } else {
      currentState++;
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
      currentState++;
    } else {
      gotoErrorState(assertMessage);
    }
    return true;
  }

  //@SuppressWarnings("unchecked")
  private void setComparatorForEvent(EventSpec eventSpec) {
    eventSpec.setComparator(comparators.get(eventSpec.getEvent().getClass()));
  }

  private boolean triggeredAnEvent() {
    Trigger trigger = triggeredEvents.get(currentState);
    if (trigger == null) {
      return false;
    }
    logger.debug("{}: triggeredAnEvent({})\t", currentState, trigger);
    trigger.doTrigger();
    currentState++;
    return true;
  }

  private boolean isStartOfBlock() {
    Block block = blockStart.get(currentState);
    if (block == null) {
      return false;
    } else {
      block.initialize();
      logger.debug("{}: repeat({})\t", currentState, block.getCurrentCount());
      currentState++;
      return true;
    }
  }

  private boolean isEndOfBlock() {
    Block block = blockEnd.get(currentState);
    if (block == null) { // not end of block
      return false;
    }

    logger.debug("{}: end({})\t", currentState, block.times);

    if (block.hasPendingEvents()) {

      while (block.hasPendingEvents()) {
        logger.debug("Awaiting pending events in empty block: {}", block.pendingEventsToString());
        EventSpec<? extends KompicsEvent> receivedSpec = removeEventFromQueue();
        logger.debug("Received ({})", receivedSpec);

        // match event
        StateTable.Transition transition = table.lookupWithBlock(currentState, receivedSpec, block);

        if (transitionedToErrorState(block.status(), receivedSpec.toString(), transition)) {
          break;
        }

        logger.debug("{}: Matched({}) with Transition({})", currentState, receivedSpec, transition);

        currentState = transition.nextState;
      }
    } else {
      block.iterationComplete();

      if (block.hasMoreIterations()) {
        currentState = block.indexOfFirstState();
      } else {
        currentState++;
      }
    }
    return true;
  }

  private void runStartState() {
    logger.debug("Sending Start to {} participant component(s)", participants.size());
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

  private void checkInBodyAndNotUnorderedMode() {
    checkInBodyMode();
    if (inUnorderedMode()) {
      throw new IllegalStateException("Not in unordered mode");
    }
  }

  private void checkInUnorderedMode() {
    if (!inUnorderedMode()) {
      throw new IllegalStateException("Not in unordered mode");
    }
  }
  private void checkInBodyMode() {
    if (currentBlock != null && currentBlock.mode != Block.MODE.BODY) {
      throw new IllegalStateException("Not in body mode");
    }
  }

  private void checkInHeaderMode() {
    if (currentBlock.mode != Block.MODE.HEADER) {
      throw new IllegalStateException("Not in header mode");
    }
  }

  private boolean inUnorderedMode() {
    return currentBlock != null && currentBlock.mode == Block.MODE.UNORDERED;
  }

  private void gotoErrorState(String errorMessage) {
    currentState = ERROR_STATE;
    ERROR_MESSAGE = errorMessage;
  }

  private EventSpec<? extends KompicsEvent> removeEventFromQueue() {
    return eventQueue.poll();
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
