package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Block {

  // // TODO: 2/17/17 make this private
  final int times;
  private final int startState;
  private int currentCount;

  private BlockInit blockInit, iterationInit;
  Block previousBlock;

  private Set<EventSpec> disallowed;
  private Set<EventSpec> allowed;
  private Set<EventSpec> dropped;

  private List<SingleEventSpec> expected = new LinkedList<SingleEventSpec>();
  private List<SingleEventSpec> pending = new LinkedList<SingleEventSpec>();
  private List<SingleEventSpec> received = new LinkedList<SingleEventSpec>();

  enum MODE { HEADER, BODY, UNORDERED, EXPECT_MAPPER, EXPECT_FUTURE}
  MODE mode = MODE.HEADER;

  Block(Block previousBlock, int times, int startState, BlockInit blockInit) {
    this(previousBlock, times, startState);
    this.blockInit = blockInit;
  }

  Block(Block previousBlock, int times, int startState) {
    this.times = times;
    this.startState = startState;
    this.previousBlock = previousBlock;

    if (previousBlock == null) {
      initEmptyBlock();
    } else {
      this.disallowed = new HashSet<EventSpec>(previousBlock.disallowed);
      this.allowed = new HashSet<EventSpec>(previousBlock.allowed);
      this.dropped = new HashSet<EventSpec>(previousBlock.dropped);
    }
  }

  void initialize() {
    currentCount = times;

    if (blockInit != null) {
      blockInit.init();
    }

    runIterationInit();
  }

  void setIterationInit(BlockInit iterationInit) {
    this.iterationInit = iterationInit;
  }

  int getCurrentCount() {
    return currentCount;
  }

  void iterationComplete() {
    currentCount--;

    if (hasMoreIterations()) {
      runIterationInit();
    }
  }

  boolean hasMoreIterations() {
    return currentCount > 0;
  }

  int indexOfFirstState() {
    return startState + 1;
  }

  void expectWithinBlock(SingleEventSpec spec) {
    expected.add(spec);
  }

  private void runIterationInit() {
    if (iterationInit != null) {
      iterationInit.init();
    }

    pending.clear();
    received.clear();
    for (SingleEventSpec spec : expected) {
      pending.add(spec);
    }
  }

  boolean handle(EventSpec receivedSpec) {
    for (Iterator<SingleEventSpec> iterator = pending.iterator(); iterator.hasNext();) {
      SingleEventSpec spec = iterator.next();
      if (spec.match(receivedSpec)) {
        received.add(spec);
        iterator.remove();
        Testkit.logger.trace("Event {} was handled by block, status: {}", receivedSpec, status());
        receivedSpec.handle();
        return true;
      }
    }
    return previousBlock != null && previousBlock.handle(receivedSpec);
  }

  boolean hasPendingEvents() {
    return !pending.isEmpty();
  }

  String pendingEventsToString() {
    StringBuilder sb = new StringBuilder("Pending<");
    for (Spec spec : pending) {
      sb.append("(").append(spec).append(") ");
    }
    sb.append(">");
    return sb.toString();
  }

  String status() {
    StringBuilder sb = new StringBuilder("Block<Received(");
    for (Spec spec : received) {
      sb.append(spec).append(" ");
    }
    sb.append(")Pending(");
    for (Spec spec : pending) {
      sb.append(spec).append(" ");
    }
    sb.append(")>");
    return sb.toString();
  }

  private void initEmptyBlock() {
    disallowed = new HashSet<EventSpec>();
    allowed = new HashSet<EventSpec>();
    dropped = new HashSet<EventSpec>();
  }

  void addDisallowedMessage(EventSpec eventSpec) {
    if (disallowed.add(eventSpec)) {
      allowed.remove(eventSpec);
      dropped.remove(eventSpec);
    }
  }

  void addAllowedMessage(EventSpec eventSpec) {
    if (allowed.add(eventSpec)) {
      disallowed.remove(eventSpec);
      dropped.remove(eventSpec);
    }
  }

  void addDroppedMessage(EventSpec eventSpec) {
    if (dropped.add(eventSpec)) {
      disallowed.remove(eventSpec);
      allowed.remove(eventSpec);
    }
  }

  Collection<EventSpec> getDisallowedEvents() {
    return disallowed;
  }
  Collection<EventSpec> getAllowedEvents() {
    return allowed;
  }
  Collection<EventSpec> getDroppedEvents() {
    return dropped;
  }

  @Override
  public String toString() {
    return "Repeat(" + times + ")";
  }
}
