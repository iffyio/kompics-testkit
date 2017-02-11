package se.sics.kompics.testkit.fsm;

class Loop extends State{

  private final int count, startIndex;
  private int currentCount;
  Loop(Env env, int count, int startIndex) {
    super(env);
    this.count = count;
    this.startIndex = startIndex;
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

  int getStartIndex() {
    return startIndex;
  }

  @Override
  protected boolean run() {
    return false;
  }
}
