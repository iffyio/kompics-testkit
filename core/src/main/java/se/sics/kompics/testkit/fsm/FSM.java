package se.sics.kompics.testkit.fsm;

import se.sics.kompics.ComponentCore;

import java.util.LinkedList;
import java.util.Queue;

public class FSM {
  Queue<State> stateQueue = new LinkedList<>();
  private ComponentCore cut;
  private boolean start = false;
  private State currentState;

  public FSM(ComponentCore cut) {
    this.cut = cut;
    stateQueue.offer(new StartState(cut));
  }

  public void addState(State state) {
    stateQueue.offer(state);
  }

  public void start() {
    if (!start) {
      start = true;
      addState(new FinalState());
      run();
    }
  }

  private void run() {
    currentState = stateQueue.peek();
    while (!stateQueue.isEmpty()) {
      boolean completed = currentState.run();
      if (completed) {
        stateQueue.poll();
        currentState = stateQueue.peek();
      }
    }
  }

}
