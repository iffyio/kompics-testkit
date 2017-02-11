package se.sics.kompics.testkit.fsm;

import se.sics.kompics.ComponentCore;
import se.sics.kompics.Kompics;
import se.sics.kompics.testkit.EventSpec;
import se.sics.kompics.testkit.Proxy;

import java.util.*;

public class FSM {
  private final EventQueue eventQueue;
  private final List<State> states = new ArrayList<>();
  private final Proxy proxy;
  private final ComponentCore proxyComponent;
  private boolean start = false;
  private final Stack<Loop> balancedRepeat = new Stack<>();
  private Env currentEnv;

  public FSM(Proxy proxy) {
    this.proxy = proxy;
    this.eventQueue = proxy.getEventQueue();
    this.proxyComponent =  proxy.getComponentCore();

    // init
    repeat(1); // initial environment
    addState(new StartState(currentEnv, proxyComponent));
  }

  public void addState(State state) {
    states.add(state);
    if (currentEnv.getStartState() == null) {
      state.setStartOfEnv();
      currentEnv.setStartState(state);
    }
  }

  public void start() {
    if (!start) {
      start = true;
      addState(new FinalState(currentEnv));
      endRepeat();
      if (!balancedRepeat.isEmpty()) {
        throw new IllegalStateException("unmatched repeat loop");
      }
      run();
    }
  }

  private void run() {
    int currentStateIndex = 0;
    State currentState;
    for (State state : states) {
      Kompics.logger.info("{}", state.getClass().getSimpleName());
    }
    //if (true) return;
    while (currentStateIndex < states.size()) {
      //Kompics.logger.info("current end state = {}", currentEnv.getEndState());
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      currentState = states.get(currentStateIndex);

      if (currentState instanceof Loop) {
        Kompics.logger.info("Start of loop, count = {}", ((Loop) currentState).getCurrentCount());
        currentEnv = currentState.getEnv();
        ((Loop) currentState).initialize();
        currentStateIndex++;
        continue;
      }

      if (currentState instanceof EndLoop) {
        EndLoop loopEnd = (EndLoop) currentState;
        Loop loopStart = loopEnd.getLoop();
        loopStart.decrementCount();
        Kompics.logger.info("End of loop, count = {}", loopStart.getCurrentCount());
        if (loopStart.getCurrentCount() > 0) {
          currentStateIndex = loopStart.getStartIndex() + 1; // first (possibly) real state in loop
          continue;
        } else {
          currentStateIndex++;
          continue;
        }
      }
      boolean completedWithoutError = currentState.run();

      if (!completedWithoutError) {
        // run error state
        break;
      } else {
        currentStateIndex++;
      }
    }
  }

  public Env getCurrentEnv() {
    return currentEnv;
  }

  EventSpec pollEventQueue() {
    return eventQueue.poll();
  }

  public void repeat(int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("negative count not allowed for repeat");
    }

    Env env = new Env(count, currentEnv);
    currentEnv = env;
    Loop loopStart = new Loop(env, count, states.size());
    addState(loopStart);
    balancedRepeat.push(loopStart);
  }

  public void endRepeat() {
    if (currentEnv.getStartState() == null) {
      throw new IllegalStateException("empty repeat blocks are not allowed");
    }

    Loop loopStart = balancedRepeat.pop();
    Env previousEnv = loopStart.getEnv();
    EndLoop loopEnd = new EndLoop(loopStart.getEnv(), loopStart);
    states.add(loopEnd);
    if (previousEnv == null) {
      throw new IllegalStateException("matching loop not found");
    }

    assert previousEnv == currentEnv;

    if (!balancedRepeat.isEmpty()) { // false only for start state
      currentEnv = balancedRepeat.peek().getEnv();
    }

    State lastAddedState = states.get(states.size() - 1);
    currentEnv.setEndState(lastAddedState);
    lastAddedState.setEndOfEnv();
  }

  public void blacklist(EventSpec eventSpec) {
    currentEnv.blacklist(eventSpec);
  }

  public void whitelist(EventSpec eventSpec) {
    currentEnv.whitelist(eventSpec);
  }

  public void conditionalDrop(EventSpec eventSpec) {
    currentEnv.conditionalDrop(eventSpec);
  }

}
