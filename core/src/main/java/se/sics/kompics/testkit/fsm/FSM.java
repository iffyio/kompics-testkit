package se.sics.kompics.testkit.fsm;

import se.sics.kompics.ComponentCore;
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

  // running fsm
  private int currentStateIndex = 0;
  private State currentState = null;

  public FSM(Proxy proxy) {
    this.proxy = proxy;
    this.eventQueue = proxy.getEventQueue();
    this.proxyComponent =  proxy.getComponentCore();

    addStartState();
  }

  private void addStartState() {
    repeat(1); // initial environment
    addStateToFSM(new StartState(proxyComponent));
  }

  public void addStateToFSM(State state) {
    state.setEnv(currentEnv);
    states.add(state);
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

  public void repeat(int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("only positive count allowed for repeat");
    }

    // replace current with new env
    currentEnv = new Env(currentEnv);

    Loop loopStart = new Loop(count, states.size());

    addStateToFSM(loopStart);
    balancedRepeat.push(loopStart);
  }


  public void start() {
    if (!start) {
      start = true;
      addFinalState();
      checkBalancedRepeatBlocks();
      run();
    }
  }

  private void addFinalState() {
    addStateToFSM(new FinalState());
    endRepeat();
  }

  private void checkBalancedRepeatBlocks() {
    if (!balancedRepeat.isEmpty()) {
      throw new IllegalStateException("unmatched end for loop");
    }
  }

  private void run() {
    while (currentStateIndex < states.size()) {
      currentState = states.get(currentStateIndex);

      if (!(startLoopWasRun() || endLoopWasRun())) { // current state is regular
        boolean completedWithoutError = currentState.run();
        if (completedWithoutError) {
          currentStateIndex++; // go to next state
        } else {
          // run error state
          break;
        }
      }
    }
  }

  private boolean startLoopWasRun() {
    boolean startLoopRan = false;
    if (currentState instanceof Loop) {
      startLoopRan = true;
      currentEnv = currentState.getEnv();
      ((Loop) currentState).initialize();
      currentStateIndex++;
    }
    return startLoopRan;
  }

  private boolean endLoopWasRun() {
    boolean endLoopRan = false;
    if (currentState instanceof EndLoop) {
      endLoopRan = true;

      EndLoop loopEnd = (EndLoop) currentState;
      loopEnd.signalIterationComplete();

      // set next state from loop end
      // if there are more iterations in the loop then go back to first state of loop
      if (loopEnd.hasMoreIterations()) {
        currentStateIndex = loopEnd.indexOfFirstState();
      } else {
        currentStateIndex++;
      }
    }
    return endLoopRan;
  }

  public void endRepeat() {
    if (balancedRepeat.isEmpty()) {
      throw new IllegalStateException("matching loop not found for end");
    } else if (currentRepeatBlockIsEmpty()) {
      throw new IllegalStateException("empty repeat blocks are not allowed");
    }

    Loop loopStart = balancedRepeat.pop();
    assert loopStart.getEnv() == currentEnv;

    EndLoop loopEnd = new EndLoop(loopStart);
    addStateToFSM(loopEnd);

    restorePreviousEnvironment();

  }

  private void restorePreviousEnvironment() {
    if (!balancedRepeat.isEmpty()) { // false only for start state
      currentEnv = balancedRepeat.peek().getEnv();
    }
  }

  private boolean currentRepeatBlockIsEmpty() {
    if (balancedRepeat.isEmpty()) {
      return true;
    } else {
      // compare current index with loopstart index
      return balancedRepeat.peek().getIndex() == states.size() - 1;
    }
  }

  EventSpec pollEventQueue() {
    return eventQueue.poll();
  }
}
