package se.sics.kompics.testkit.fsm;

import se.sics.kompics.testkit.EventSpec;

import java.util.HashSet;
import java.util.Set;

class Env {

  private Set<EventSpec> blacklist;
  private Set<EventSpec> whitelist;
  private Set<EventSpec> conditionalDrop;

  Env(Env previousEnv) {
    if (previousEnv == null) {
      initEnv();
    } else {
      this.blacklist = new HashSet<>(previousEnv.getBlacklist());
      this.whitelist = new HashSet<>(previousEnv.getWhitelist());
      this.conditionalDrop = previousEnv.getConditionalDrop();
    }
  }

  private void initEnv() {
    blacklist = new HashSet<>();
    whitelist = new HashSet<>();
    conditionalDrop = new HashSet<>();
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
