package se.sics.kompics.testkit;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import se.sics.kompics.KompicsEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Block {

  final int times;
  private final int startState;
  private int currentCount;

  private BlockInit blockInit, iterationInit;
  final Block previousBlock;

  private Set<EventSpec> disallowed;
  private Set<EventSpec> allowed;
  private Set<EventSpec> dropped;

  private List<SingleEventSpec> expected = new LinkedList<SingleEventSpec>();
  private Multiset<SingleEventSpec> pending = HashMultiset.create();
  private Multiset<SingleEventSpec> received = HashMultiset.create();

  enum MODE { HEADER, BODY, UNORDERED, EXPECT_MAPPER, EXPECT_FUTURE, CONDITIONAL}
  MODE mode = MODE.HEADER;
  MODE previousMode;

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

  StateTable.Transition getTransition(int state, EventSpec receivedSpec) {
    //// TODO: 3/23/17 merge with state onEvent getTransition
    if (handle(receivedSpec)) {
      return new StateTable.Transition(receivedSpec, Action.HANDLE, state);
    }

    Testkit.logger.debug("{}: looking up {} with block {}", state, receivedSpec, status());
    if (allowed.contains(receivedSpec)) {
      return new StateTable.Transition(receivedSpec, Action.HANDLE, state);
    }
    if (dropped.contains(receivedSpec)) {
      return new StateTable.Transition(receivedSpec, Action.DROP, state);
    }
    if (disallowed.contains(receivedSpec)) {
      return new StateTable.Transition(receivedSpec, Action.FAIL, FSM.ERROR_STATE);
    }

    return null;
  }

  boolean handle(EventSpec receivedSpec) {
    int remaining = pending.count(receivedSpec);
    if (remaining == 0) {
      return previousBlock != null && previousBlock.handle(receivedSpec);
    }

    pending.remove(receivedSpec);
    Testkit.logger.trace("Event {} will be handled by {}", receivedSpec, status());
    return true;
  }

  boolean hasPendingEvents() {
    return !pending.isEmpty();
  }

  String status() {
    StringBuilder sb = new StringBuilder("Block[");
    sb.append(times).append(" Received").append(received);
    sb.append(" Pending").append(pending);
    sb.append("]");
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

  @Override
  public String toString() {
    return "Repeat(" + times + ")";
  }
}
