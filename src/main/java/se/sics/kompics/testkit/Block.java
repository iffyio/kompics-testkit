package se.sics.kompics.testkit;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Block {

  final int times;
  private int startState;
  private int currentCount;
  static final int STAR = -1;

  private boolean isKleeneBlock;

  private BlockInit blockInit, iterationInit;
  final Block previousBlock;

  boolean currentlyExecuting;
  boolean iterationInitHasRun;

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
    throw new UnsupportedOperationException("deprecate start state");
  }

  Block(Block previousBlock, int count, BlockInit blockInit) {
    this(previousBlock, count);
    this.blockInit = blockInit;
  }

  Block(Block previousBlock, int count) {
    this.times = count;

    if (count == STAR) {
      isKleeneBlock = true;
    }

    this.previousBlock = previousBlock;

    if (previousBlock == null) {
      initEmptyBlock();
    } else {
      this.disallowed = new HashSet<EventSpec>(previousBlock.disallowed);
      this.allowed = new HashSet<EventSpec>(previousBlock.allowed);
      this.dropped = new HashSet<EventSpec>(previousBlock.dropped);
    }
  }

  Block(Block previousBlock, int times, int startState) {
    this(previousBlock, times);
    this.startState = startState;
    throw new UnsupportedOperationException("deprecate block");
  }

  void setIterationInit(BlockInit iterationInit) {
    this.iterationInit = iterationInit;
  }

  int getCurrentCount() {
    return currentCount;
  }

  void initialize() {
    assert !currentlyExecuting; // don't try to initialize a block that isn't done executing
    assert !iterationInitHasRun;

    currentlyExecuting = true;

    if (!isKleeneBlock) {
      currentCount = times;
    }

    if (blockInit != null) {
      blockInit.init();
    }

    runIterationInit();
  }

  void iterationComplete() {
    assert iterationInitHasRun;
    iterationInitHasRun = false;

    assert pending.isEmpty();
    resetBlockEvents();

    if (!isKleeneBlock) {
      currentCount--;
      assert currentCount >= 0;
    }
  }

  void reset() {
    if (isKleeneBlock) {
      // without explicit end conditions -
      // kleene blocks only go out of scope when the thread is discontinued by the NFA
      // close the block to re-enable block init to run later if needed
      currentlyExecuting = false;
    }
    resetBlockEvents();
  }

  private void resetBlockEvents() {
    pending.clear();
    received.clear();

    for (SingleEventSpec spec : expected) {
      pending.add(spec);
    }
  }

  boolean hasMoreIterations() {
    return currentCount > 0;
  }

  int indexOfFirstState() {
    return startState + 1;
  }

  void expect(SingleEventSpec spec) {
    expected.add(spec);
    pending.add(spec);
  }

  void runIterationInit() {
    assert !iterationInitHasRun;
    iterationInitHasRun = true;
    if (iterationInit != null) {
      iterationInit.init();
    }
  }

  StateTable.Transition getTransition(int state, EventSpec receivedSpec) {
    if (handle(receivedSpec)) {
      return new StateTable.Transition(Action.HANDLE, state);
    }

    Testkit.logger.debug("{}: looking up {} with block {}", state, receivedSpec, status());
    if (allowed.contains(receivedSpec)) {
      return new StateTable.Transition(Action.HANDLE, state);
    }
    if (dropped.contains(receivedSpec)) {
      return new StateTable.Transition(Action.DROP, state);
    }
    if (disallowed.contains(receivedSpec)) {
      return new StateTable.Transition(Action.FAIL, FSM.ERROR_STATE);
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
    sb.append(times == STAR? "*" : times).append(" Received").append(received);
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

  Collection<EventSpec> getAllowedSpecs() {
    return allowed;
  }

  Collection<EventSpec> getDisallowedSpecs() {
    return disallowed;
  }
  Collection<EventSpec> getDroppedSpecs() {
    return dropped;
  }

  @Override
  public String toString() {
    return "Repeat(" + (times == STAR? "*" : times) + ")";
  }
}
