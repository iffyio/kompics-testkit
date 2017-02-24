package se.sics.kompics.testkit.fsm;

import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.testkit.Direction;
import se.sics.kompics.testkit.Proxy;
import se.sics.kompics.testkit.TestContext;

import java.util.*;


public class FSM {
  static final Logger logger = LoggerFactory.getLogger(FSM.class);

  private final TestContext testContext;
  static final int ERROR_STATE = -1;
  private int FINAL_STATE;
  private boolean STARTED = false;
  private String ERROR_MESSAGE = "";

  private final ComponentCore proxyComponent;
  private Collection<Component> participants = new HashSet<>();
  private final Stack<Block> balancedRepeat = new Stack<>();
  private final EventQueue eventQueue;

  private Map<Integer, Repeat> loops = new HashMap<>();
  private Map<Integer, Repeat> end = new HashMap<>();
  private Map<Integer, Trigger> triggeredEvents = new HashMap<>();
  private Map<Integer, Predicate<? extends ComponentDefinition>> assertPredicates = new HashMap<>();

  private ComparatorMap comparators = new ComparatorMap();
  private StateTable table = new StateTable();

  private Block currentBlock;
  private int currentState = 0;

  public FSM(Proxy proxy, TestContext testContext) {
    this.eventQueue = proxy.getEventQueue();
    this.proxyComponent =  proxy.getComponentCore();
    this.testContext = testContext;

    initializeFSM();
  }

  private void initializeFSM() {
    repeat(1);
  }

  public void addParticipatingComponents(Component c) {
    participants.add(c);
  }

  public <P extends  PortType> void addDisallowedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    assertInHeader();
    currentBlock.env.addDisallowedMessage(newEventSpec(event, port, direction));
  }

  public <P extends  PortType> void addAllowedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    assertInHeader();
    currentBlock.env.addAllowedMessage(newEventSpec(event, port, direction));
  }

  public <P extends  PortType> void addDroppedEvent(
          KompicsEvent event, Port<P> port, Direction direction) {
    assertInHeader();
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
    assertInBody();

    if (times <= 0) {
      throw new IllegalArgumentException("only positive value allowed for repeat");
    }

    Repeat repeat = new Repeat(times, currentState);

    currentBlock = new Block(repeat, currentBlock);
    balancedRepeat.push(currentBlock);

    loops.put(currentState, repeat);
    currentState++;
  }

  public void body() {
    assertInHeader();
    currentBlock.inHeaderMode = false;
  }

  public void addTrigger(KompicsEvent event, Port<? extends PortType> port) {
    assertInBody();
    triggeredEvents.put(currentState, new Trigger(event, port));
    currentState++;
  }

  public void endRepeat() {
    assertInBody();

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
    // compare current index with startOfLoop index
    return  balancedRepeat.isEmpty() ||
            // no state was added since start of loop
            balancedRepeat.peek().repeat.getIndex() == currentState - 1;
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
    assertInHeader();
    if (currentBlock.previousBlock != null) { // not main repeat block
      throw new IllegalStateException("Comparators are only allowed in the main header");
    }
    comparators.put(eventType, comparator);
  }

  public void addAssertComponent(Predicate<? extends ComponentDefinition> assertPred) {
    assertPredicates.put(currentState, assertPred);
    currentState++;
  }

  public <P extends PortType> void expectMessage(KompicsEvent event, Port<P> port, Direction direction) {
    assertInBody();
    EventSpec eventSpec = newEventSpec(event, port, direction);
    table.registerExpectedEvent(currentState, eventSpec, currentBlock.env);
    currentState++;
  }

  public <P extends PortType, E extends KompicsEvent> void expectMessage(
          Class<E> eventType, Predicate<E> pred, Port<P> port, Direction direction) {
    assertInBody();
    PredicateSpec predicateSpec = new PredicateSpec(eventType, pred, port, direction);
    table.registerExpectedEvent(currentState, predicateSpec, currentBlock.env);
    currentState++;
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

        StateTable.Action action = table.lookup(currentState, received);

        if (!transitionedToErrorState(expected, received, action)) {

          logger.warn("{}: Matched ({}) with Action = {}",
                  currentState, received, action);

          if (action.handleEvent()) {
            received.handle();
          }

          currentState = action.nextIndex;
        }
      }
    }

    runFinalState();
  }

  private boolean transitionedToErrorState(
          Spec expected, EventSpec received, StateTable.Action action) {
    if (action != null && action.nextIndex != ERROR_STATE) {
      return false;
    }

    gotoErrorState();
    ERROR_MESSAGE = String.format(
            "Received %s message <%s> while expecting <%s>",
            (action == null? "unexpected" : "unwanted"), received, expected);
    return true;
  }

  private boolean assertedComponent() {
    Predicate assertPred = assertPredicates.get(currentState);
    if (assertPred == null) {
      return false;
    }

    logger.warn("{}: Asserting Component", currentState);
    ComponentDefinition definitionUnderTest = testContext.getDefinitionUnderTest();
    //// TODO: 2/22/17 generify assertPred properly
    boolean successful = assertPred.apply(definitionUnderTest);

    if (!successful) {
      gotoErrorState();
    } else {
      currentState++;
    }

    return true;
  }

  @SuppressWarnings("unchecked")
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

  private void assertInBody() {
    if (currentBlock != null && currentBlock.inHeaderMode) {
      throw new IllegalStateException("Not in body mode");
    }
  }

  private void assertInHeader() {
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
    Map<Class<? extends KompicsEvent>, Comparator<? extends KompicsEvent>> comparators = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <E extends KompicsEvent> Comparator<E> get(Class<E> eventType) {
      return (Comparator<E>) comparators.get(eventType);
    }

    public <E extends KompicsEvent> void put(Class<E> eventType, Comparator<E> comparator) {
      comparators.put(eventType, comparator);
    }
  }
}
