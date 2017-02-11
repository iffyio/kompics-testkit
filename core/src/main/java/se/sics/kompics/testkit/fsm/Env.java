package se.sics.kompics.testkit.fsm;

import se.sics.kompics.testkit.EventSpec;

import java.util.HashSet;
import java.util.Set;

public class Env {

  private final int count;
  private int currentCount;
  private State startState;
  private State endState;
  private Set<EventSpec> blacklist;
  private Set<EventSpec> whitelist;
  private Set<EventSpec> conditionalDrop;
  private Env previousEnv;

  public Env(int count, Env previousEnv) {
    this.count = count;
    this.previousEnv = previousEnv;
    if (previousEnv == null) {
      initEnv();
    } else {
      this.blacklist = new HashSet<>(previousEnv.getBlacklist());
      this.whitelist = new HashSet<>(previousEnv.getWhitelist());
      this.conditionalDrop = previousEnv.getConditionalDrop();
    }
  }

  public Env(int count) {
    this.count = count;
    initEnv();
  }

  private void initEnv() {
    blacklist = new HashSet<>();
    whitelist = new HashSet<>();
    conditionalDrop = new HashSet<>();
  }

  public void initialize() {
    currentCount = count;
  }

  int getCurrentCount() {
    return currentCount;
  }

  void decrementCurrentCount() {
    currentCount--;
    assert currentCount >= 0;
  }

  Env getPreviousEnv() {
    return previousEnv;
  }

  void setStartState(State startState) {
    this.startState = startState;
  }

  State getStartState() {
    return startState;
  }

  void setEndState(State endState) {
    this.endState = endState;
  }

  State getEndState() {
    return endState;
  }

  void blacklist(EventSpec eventSpec) {
    if (blacklist.add(eventSpec)) {
      whitelist.remove(eventSpec);
      conditionalDrop.remove(eventSpec);
    }
  }

  void whitelist(EventSpec eventSpec) {
    if (whitelist.add(eventSpec)) {
      blacklist.remove(eventSpec);
      conditionalDrop.remove(eventSpec);
    }
  }

  void conditionalDrop(EventSpec eventSpec) {
    if (conditionalDrop.add(eventSpec)) {
      blacklist.remove(eventSpec);
      whitelist.remove(eventSpec);
    }
  }

  Set<EventSpec> getBlacklist() {
    return blacklist;
  }

  Set<EventSpec> getWhitelist() {
    return whitelist;
  }

  Set<EventSpec> getConditionalDrop() {
    return conditionalDrop;
  }

}
