package se.sics.kompics.testkit.fsm;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.PortType;
import se.sics.kompics.Port;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Start;

import se.sics.kompics.testkit.Action;
import se.sics.kompics.testkit.Direction;
import se.sics.kompics.testkit.LoopInit;
import se.sics.kompics.testkit.Proxy;

public class FSM<T extends ComponentDefinition> {
  static final Logger logger = LoggerFactory.getLogger(FSM.class);

  private final T definitionUnderTest;
  static final int ERROR_STATE = -1;
  private int FINAL_STATE;
  private boolean STARTED = false;
  private String ERROR_MESSAGE = "";

  private final ComponentCore proxyComponent;
  private Collection<Component> participants = new HashSet<Component>();
  private final Stack<Block> balancedRepeat = new Stack<Block>();
  private final EventQueue eventQueue;

  private Map<Integer, Repeat> loops = new HashMap<Integer, Repeat>();
  private Map<Integer, Repeat> end = new HashMap<Integer, Repeat>();
  private Map<Integer, Trigger> triggeredEvents = new HashMap<Integer, Trigger>();
  private Map<Integer, Predicate<T>> assertPredicates = new HashMap<Integer, Predicate<T>>();

  private ComparatorMap comparators = new ComparatorMap();
  private StateTable table = new StateTable();

  private Block currentBlock;
  private int currentState = 0;

  public FSM(Proxy proxy, T definition) {
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
    currentBlock.env.addDisallowedMessage(newEventSpec(event, port, direction));
  }

  public <P extends  PortType> void addAllowedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    checkInHeaderMode();
    currentBlock.env.addAllowedMessage(newEventSpec(event, port, direction));
  }

  public <P extends  PortType> void addDroppedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    checkInHeaderMode();
    currentBlock.env.addDroppedMessage(newEventSpec(event, port, direction));
  }

  private  <P extends  PortType, E extends KompicsEvent> EventSpec newEventSpec(
          KompicsEvent event, Port<P> port, Direction direction) {
    Comparator<E> c = (Comparator<E>) comparators.get(event.getClass());
    return new EventSpec<>((E) event, port, direction, c);
  }

  public int getFinalState() {
    return STARTED? FINAL_STATE : currentState;
  }

  public void repeat(int times) {
    Repeat repeat = new Repeat(times, currentState);
    addRepeat(repeat);
  }

  public void repeat(int times, LoopInit init) {
    Repeat repeat = new Repeat(times, currentState, init);
    addRepeat(repeat);
  }

  private void addRepeat(Repeat repeat) {
    checkInBodyMode();

    if (repeat.times <= 0) {
      throw new IllegalArgumentException("only positive value allowed for repeat");
    }

    currentBlock = new Block(repeat, currentBlock);
    balancedRepeat.push(currentBlock);

    loops.put(currentState, repeat);
    currentState++;
  }

  public void setIterationInit(LoopInit iterationInit) {
    checkInHeaderMode();
    currentBlock.repeat.setIterationInit(iterationInit);
  }

  public void body() {
    checkInHeaderMode();
    currentBlock.inHeaderMode = false;
  }

  public void addTrigger(KompicsEvent event, Port<? extends PortType> port) {
    checkInBodyMode();
    triggeredEvents.put(currentState, new Trigger(event, port));
    currentState++;
  }

  public void endRepeat() {
    checkInBodyMode();

    if (balancedRepeat.isEmpty()) {
      throw new IllegalStateException("matching repeat not found for end");
    } else if (currentRepeatBlockIsEmpty()) {
      throw new IllegalStateException("empty repeat blocks are not allowed");
    }

    Repeat loopHead = currentBlock.repeat;
    end.put(currentState, loopHead);

    restorePreviousBlock();
    currentState++;
  }

  private void restorePreviousBlock() {
    if (!balancedRepeat.isEmpty()) { // false only for initial block
      currentBlock = balancedRepeat.pop().previousBlock;
    }
  }

  private boolean currentRepeatBlockIsEmpty() {
    // compare current stateIndex with startOfLoop stateIndex
    return  balancedRepeat.isEmpty() ||
            // no state was added since start of loop
            balancedRepeat.peek().repeat.getStateIndex() == currentState - 1;
  }


  public int start() {
    if (!STARTED) {
      STARTED = true;
      addFinalState();
      checkBalancedRepeatBlocks();
      table.printTable(FINAL_STATE);
      run();
    }
    return currentState;
  }

  private void addFinalState() { FINAL_STATE = currentState;
    endRepeat();
  }

  public <E extends KompicsEvent> void addComparator(
          Class<E> eventType, Comparator<E> comparator) {
    checkInInitialHeader();
    comparators.put(eventType, comparator);
  }

  public void addAssertComponent(Predicate<T> assertPred) {
    assertPredicates.put(currentState, assertPred);
    currentState++;
  }

  public <P extends PortType> void expectMessage(
          KompicsEvent event, Port<P> port, Direction direction) {
    checkInBodyMode();
    EventSpec eventSpec = newEventSpec(event, port, direction);
    table.registerExpectedEvent(currentState, eventSpec, currentBlock.env);
    currentState++;
  }

  public <P extends PortType, E extends KompicsEvent> void expectMessage(
          Class<E> eventType, Predicate<E> pred, Port<P> port, Direction direction) {
    checkInBodyMode();
    PredicateSpec predicateSpec = new PredicateSpec(eventType, pred, port, direction);
    table.registerExpectedEvent(currentState, predicateSpec, currentBlock.env);
    currentState++;
  }

  public <E extends KompicsEvent> void setDefaultAction(
          Class<E> eventType, Function<E, Action> function) {
    checkInInitialHeader();
    table.setDefaultAction(eventType, function);
  }

  private void run() {
    runStartState();

    currentState = 0;
    while (currentState < FINAL_STATE && currentState != ERROR_STATE) {
      if (!(startOfLoop() || endOfLoop() || triggeredAnEvent() || assertedComponent())) {
        // expecting an event
        table.printExpectedEventAt(currentState);
        Spec expected = table.getExpectedSpecAt(currentState);

        EventSpec received = removeEventFromQueue();
        setComparatorForEvent(received);

        StateTable.Transition transition = table.lookup(currentState, received);

        if (!transitionedToErrorState(expected, received, transition)) {

          logger.warn("{}: Matched ({}) with Transition = {}",
                  currentState, received, transition);

          if (transition.handleEvent()) {
            received.handle();
          }

          currentState = transition.nextState;
        }
      }
    }

    runFinalState();
  }

  private boolean transitionedToErrorState(
          Spec expected, EventSpec received, StateTable.Transition transition) {
    if (transition != null && transition.nextState != ERROR_STATE) {
      return false;
    }

    gotoErrorState();
    ERROR_MESSAGE =
            String.format("Received %s message <%s> while expecting <%s>",
            (transition == null? "unexpected" : "unwanted"), received, expected);
    return true;
  }

  private boolean assertedComponent() {
    Predicate<T> assertPred = assertPredicates.get(currentState);
    if (assertPred == null) {
      return false;
    }

    logger.warn("{}: Asserting Component", currentState);
    boolean successful = assertPred.apply(definitionUnderTest);

    if (!successful) {
      gotoErrorState();
    } else {
      currentState++;
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

    logger.warn("{}: triggeredAnEvent({})\t", currentState, trigger);
    trigger.doTrigger();
    currentState++;
    return true;
  }

  private boolean startOfLoop() {
    Repeat loop = loops.get(currentState);
    if (loop == null) {
      return false;
    } else {
      loop.initialize();

      logger.warn("{}: repeat({})\t", currentState, loop.getCurrentCount());

      currentState++;
      return true;
    }
  }

  private boolean endOfLoop() {
    Repeat loop = end.get(currentState);
    if (loop == null) {
      return false;
    }

    logger.warn("{}: end({})\t", currentState, loop.times);

    loop.iterationComplete();

    if (loop.hasMoreIterations()) {
      currentState = loop.indexOfFirstState();
    } else {
      currentState++;
    }

    return true;
  }

  private void runStartState() {
    logger.warn("Sending Start to {} participant component(s)", participants.size());
    for (Component child : participants) {
      child.getControl().doTrigger(Start.event, 0, proxyComponent);
    }
  }

  private void runFinalState() {
    logger.warn("Done!({})", currentState == ERROR_STATE?
            "FAILED -> " + ERROR_MESSAGE : "PASS");
  }

  private void checkBalancedRepeatBlocks() {
    if (!balancedRepeat.isEmpty()) {
      throw new IllegalStateException("unmatched end for loop");
    }
  }

  public void checkInInitialHeader() {
    checkInHeaderMode();
    if (currentBlock == null || currentBlock.previousBlock != null) {
      throw new IllegalStateException("Operation only supported in initial header");
    }
  }

  private void checkInBodyMode() {
    if (currentBlock != null && currentBlock.inHeaderMode) {
      throw new IllegalStateException("Not in body mode");
    }
  }

  private void checkInHeaderMode() {
    if (!currentBlock.inHeaderMode) {
      throw new IllegalStateException("Not in header mode");
    }
  }

  private void gotoErrorState() {
    currentState = ERROR_STATE;
  }

  private EventSpec removeEventFromQueue() {
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

  private class Block {
    boolean inHeaderMode = true;
    final Repeat repeat;
    final Block previousBlock;
    Environment env;

    Block(Repeat repeat, Block previousBlock) {
      this.repeat = repeat;
      this.previousBlock = previousBlock;

      if (previousBlock == null) {
        env = new Environment(null);
      } else {
        env = new Environment(previousBlock.env);
      }
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
