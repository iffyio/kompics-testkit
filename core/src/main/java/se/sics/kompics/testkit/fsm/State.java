package se.sics.kompics.testkit.fsm;

import se.sics.kompics.ComponentCore;

abstract class State {
  protected abstract boolean run();
/*  protected ComponentCore component;*/
/*  protected State(ComponentCore component) {
    this.component = component;
  }*/
}
