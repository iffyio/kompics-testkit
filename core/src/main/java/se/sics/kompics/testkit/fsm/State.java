package se.sics.kompics.testkit.fsm;

abstract class State {

  Env env;

  protected void setEnv(Env env) {
    this.env = env;
  }

  protected Env getEnv() {
    return env;
  }

  protected abstract boolean run();
}
