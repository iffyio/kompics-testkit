package se.sics.kompics.testkit.fsm;

import se.sics.kompics.testkit.LoopInit;

class Repeat {

  // // TODO: 2/17/17 make this private
  final int times, stateIndex;
  private int currentCount;
  private LoopInit loopInit, iterationInit;

  Repeat(int times, int stateIndex) {
    this.times = times;
    this.stateIndex = stateIndex;
  }

  Repeat(int times, int stateIndex, LoopInit loopInit) {
    this(times, stateIndex);
    this.loopInit = loopInit;
  }

  void initialize() {
    currentCount = times;

    if (loopInit != null) {
      loopInit.init();
    }

    runIterationInit();
  }

  void setIterationInit(LoopInit iterationInit) {
    this.iterationInit = iterationInit;
  }

  int getCurrentCount() {
    return currentCount;
  }

  int getStateIndex() {
    return stateIndex;
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
    return stateIndex + 1;
  }

  private void runIterationInit() {
    if (iterationInit != null) {
      iterationInit.init();
    }
  }
}
