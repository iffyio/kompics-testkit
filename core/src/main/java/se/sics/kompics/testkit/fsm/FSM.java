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
  private StateTable currentTable;

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
    repeat(1); // set initial state table
    addStateToFSM(new StartState(proxyComponent));
  }

  public void addStateToFSM(State state) {
    state.setFsm(this);
    state.setStateTable(currentTable);
    states.add(state);
  }

  public void blacklist(EventSpec eventSpec) {
    currentTable.blacklist(eventSpec);
  }

  public void whitelist(EventSpec eventSpec) {
    currentTable.whitelist(eventSpec);
  }

  public void conditionalDrop(EventSpec eventSpec) {
    currentTable.conditionallyDrop(eventSpec);
  }

  public void repeat(int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("only positive count allowed for repeat");
    }

    // replace current with new stateTable
    currentTable = new StateTable(currentTable);

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
      //Kompics.logger.info("current end state = {}", currentTable.getEndState());
/*      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }*/
      currentState = states.get(currentStateIndex);

      if (!(startLoopWasRun() || endLoopWasRun())) { // current state is regular
        // run self and error transitions
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
      currentTable = currentState.getStateTable();
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
    assert loopStart.getStateTable() == currentTable;

    EndLoop loopEnd = new EndLoop(loopStart);
    addStateToFSM(loopEnd);

    restorePreviousStateTable();

  }

  private void restorePreviousStateTable() {
    if (!balancedRepeat.isEmpty()) { // false only for start state
      currentTable = balancedRepeat.peek().getStateTable();
    }
  }

  private boolean currentRepeatBlockIsEmpty() {
    // compare current index with loopstart index
    return  balancedRepeat.isEmpty() ||
            balancedRepeat.peek().getIndex() == states.size() - 1;
  }

  EventSpec peekEventQueue() {
    return eventQueue.peek();
  }
  EventSpec removeEventFromQueue() {
    return eventQueue.poll();
  }
}
