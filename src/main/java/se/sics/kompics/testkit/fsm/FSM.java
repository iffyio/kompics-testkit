package se.sics.kompics.testkit.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.testkit.Proxy;
import se.sics.kompics.testkit.TestKit;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


public class FSM {
  private static final Logger logger = LoggerFactory.getLogger(FSM.class);

  static final int ERROR_STATE = -1;
  private int FINAL_STATE;
  private String errorMessage = "";

  private final EventQueue eventQueue;
  private final ComponentCore proxyComponent;
  private boolean start = false;
  private final Stack<Block> balancedRepeat = new Stack<>();

  private Map<Integer, Repeat> loops = new HashMap<>();
  private Map<Integer, Repeat> end = new HashMap<>();
  private Map<Integer, Trigger> triggeredEvents = new HashMap<>();
  private Map<Integer, EventSpec> expectedEvents = new HashMap<>();

  private ComparatorMap comparators = new ComparatorMap();
  private StateTable table = new StateTable();

  private Block currentBlock;

  private int currentStateIndex = 0;

  public FSM(Proxy proxy) {
    this.eventQueue = proxy.getEventQueue();
    this.proxyComponent =  proxy.getComponentCore();

    initializeFSM();
  }

  private void initializeFSM() {
    repeat(1);
  }


  public <P extends  PortType, E extends KompicsEvent> void addDisallowedEvent(
          KompicsEvent event, Port<P> port, TestKit.Direction direction) {
    currentBlock.env.addDisallowedMessage(newEventSpec(event, port, direction));
  }

  public <P extends  PortType> void addAllowedEvent(
          KompicsEvent event, Port<P> port, TestKit.Direction direction) {
    currentBlock.env.addAllowedMessage(newEventSpec(event, port, direction));
  }

  public <P extends  PortType> void addDroppedEvent(
          KompicsEvent event, Port<P> port, TestKit.Direction direction) {
    currentBlock.env.addDroppedMessage(newEventSpec(event, port, direction));
  }

  @SuppressWarnings("unchecked")
  private  <P extends  PortType, E extends KompicsEvent> EventSpec newEventSpec(
          KompicsEvent event, Port<P> port, TestKit.Direction direction) {
    Comparator<E> c = (Comparator<E>) comparators.get(event.getClass());
    return new EventSpec<E>((E) event, port, direction, c);
  }


  public void repeat(int times) {
    assertInBody();

    if (times <= 0) {
      throw new IllegalArgumentException("only positive value allowed for repeat");
    }

    Repeat repeat = new Repeat(times, currentStateIndex);

    currentBlock = new Block(repeat, currentBlock);
    balancedRepeat.push(currentBlock);

    loops.put(currentStateIndex, repeat);
    currentStateIndex++;
  }

  public void body() {
    assertInHeader();
    currentBlock.inHeaderMode = false;
  }

  public void addTrigger(KompicsEvent event, Port<? extends PortType> port) {
    assertInBody();
    triggeredEvents.put(currentStateIndex, new Trigger(event, port));
    currentStateIndex++;
  }

  public void endRepeat() {
    assertInBody();

    if (balancedRepeat.isEmpty()) {
      throw new IllegalStateException("matching repeat not found for end");
    } else if (currentRepeatBlockIsEmpty()) {
      throw new IllegalStateException("empty repeat blocks are not allowed");
    }

    Repeat loopHead = currentBlock.repeat;
    end.put(currentStateIndex, loopHead);

    restorePreviousBlock();
    currentStateIndex++;
  }

  private void restorePreviousBlock() {
    if (!balancedRepeat.isEmpty()) { // false only for initial block
      currentBlock = balancedRepeat.pop().previousBlock;
    }
  }

  private boolean currentRepeatBlockIsEmpty() {
    // compare current index with startOfLoop index
    return  balancedRepeat.isEmpty() ||
            balancedRepeat.peek().repeat.getIndex() == currentStateIndex - 1;
  }


  public void start() {
    if (!start) {
      start = true;
      addFinalState();
      checkBalancedRepeatBlocks();
      table.printTable(FINAL_STATE);
      run();
    }
  }

  private void addFinalState() {
    FINAL_STATE = currentStateIndex;
    endRepeat();
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

  public <E extends KompicsEvent> void addComparator(
          Class<E> eventType, Comparator<E> comparator) {
    assertInHeader();
    if (currentBlock.previousBlock != null) { // not main repeat block
      throw new IllegalStateException("Comparators are only allowed in the main header");
    }
    comparators.put(eventType, comparator);
  }

  public <P extends PortType> void expectMessage(KompicsEvent event, Port<P> port, TestKit.Direction direction) {
    assertInBody();
    EventSpec eventSpec = newEventSpec(event, port, direction);
    int nextState = currentStateIndex + 1;
    table.addStateClause(currentStateIndex, eventSpec, nextState, currentBlock.env);
    expectedEvents.put(currentStateIndex, eventSpec);
    currentStateIndex++;
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


  private void run() {
    runStartState();
    currentStateIndex = 0;

    while (currentStateIndex < FINAL_STATE && currentStateIndex != ERROR_STATE) {
      if (!(startOfLoop() || endOfLoop() || triggerAction())) {
        //// TODO: 2/17/17 remove this
        EventSpec e = expectedEvents.get(currentStateIndex);
        logger.warn("{}: Expect\t{}", currentStateIndex, e);
        EventSpec eventSpec = removeEventFromQueue();
        StateTable.Action action = table.lookUp(currentStateIndex, eventSpec);
        if (action.handleEvent()) {
          eventSpec.handle();
        }
        logger.warn("{}: Matched ({}) with Action = {}",
                currentStateIndex, eventSpec, action);
        currentStateIndex = action.nextIndex;
        if (currentStateIndex == ERROR_STATE) {
          errorMessage = String.format(
                  "Received unexpected message <%s> while expecting <%s>", eventSpec, e);
        }
      }
    }

    runFinalState();
  }

  private boolean triggerAction() {
    Trigger trigger = triggeredEvents.get(currentStateIndex);

    if (trigger == null) {
      return false;
    }

    logger.warn("{}: triggerAction({})\t", currentStateIndex, trigger);
    trigger.doTrigger();
    currentStateIndex++;
    return true;
  }

  private boolean startOfLoop() {
    Repeat loop = loops.get(currentStateIndex);
    if (loop == null) {
      return false;
    } else {
      loop.initialize();
      logger.warn("{}: repeat({})\t", currentStateIndex, loop.getCurrentCount());
      currentStateIndex++;
      return true;
    }
  }

  private boolean endOfLoop() {
    Repeat loop = end.get(currentStateIndex);
    if (loop == null) {
      return false;
    }

    logger.warn("{}: end({})\t", currentStateIndex, loop.times);
    loop.iterationComplete();
    if (loop.hasMoreIterations()) {
      currentStateIndex = loop.indexOfFirstState();
    } else {
      currentStateIndex++;
    }
    return true;
  }

  private void runStartState() {
    logger.warn("Sending Start to component...");
    proxyComponent.getControl().doTrigger(Start.event, 0, proxyComponent);
  }

  private void runFinalState() {
    logger.warn("Done!({})", currentStateIndex == ERROR_STATE?
            "FAILED -> " + errorMessage : "PASS");
  }

  private EventSpec removeEventFromQueue() {
    return eventQueue.poll();
  }

}
