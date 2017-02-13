package se.sics.kompics.testkit.fsm;

class Loop extends State{

  private final int count, index;
  private int currentCount;
  Loop(int count, int index) {
    this.count = count;
    this.index = index;
  }

  void initialize() {
    currentCount = count;
  }

  void decrementCount() {
    currentCount--;
  }

  int getCurrentCount() {
    return currentCount;
  }

  int getIndex() {
    return index;
  }

}
