package se.sics.kompics.testkit.fsm;

public class EndLoop extends State{

  private final Loop loop;

  protected EndLoop(Env env, Loop loop) {
    super(env);
    this.loop = loop;
  }

  Loop getLoop() {
    return loop;
  }

  @Override
  protected boolean run() {
    return false;
  }
}
