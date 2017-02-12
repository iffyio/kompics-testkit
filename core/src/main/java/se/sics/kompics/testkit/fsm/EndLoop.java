package se.sics.kompics.testkit.fsm;

class EndLoop extends State{

  private final Loop startLoop;

  EndLoop(Loop startLoop) {
    this.startLoop = startLoop;
  }

  void signalIterationComplete() {
    startLoop.decrementCount();
  }

  boolean hasMoreIterations() {
    return startLoop.getCurrentCount() > 0;
  }

  int indexOfFirstState() {
    return startLoop.getIndex() + 1;
  }

  @Override
  protected boolean run() {
    return false;
  }
}
