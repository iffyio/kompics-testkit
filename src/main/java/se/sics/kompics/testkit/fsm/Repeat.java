package se.sics.kompics.testkit.fsm;

class Repeat {

  // // TODO: 2/17/17 make this private
  final int times, index;
  private int currentCount;

  Repeat(int times, int index) {
    this.times = times;
    this.index = index;
  }

  void initialize() {
    currentCount = times;
  }

  int getCurrentCount() {
    return currentCount;
  }

  int getIndex() {
    return index;
  }

  void iterationComplete() {
    currentCount--;
  }

  boolean hasMoreIterations() {
    return currentCount > 0;
  }

  int indexOfFirstState() {
    return index + 1;
  }
}
