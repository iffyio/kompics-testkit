package se.sics.kompics.testkit.fsm;

import se.sics.kompics.Kompics;
import se.sics.kompics.testkit.EventSpec;

import java.util.HashSet;
import java.util.Set;

class StateTable {

  private Set<EventSpec> blacklist; // events that transition to error state
  private Set<EventSpec> whitelist; // events that self-transition
  private Set<EventSpec> conditionalDrop;

  StateTable(StateTable previousTable) {
    if (previousTable == null) {
      initTables();
    } else {
      this.blacklist = new HashSet<>(previousTable.getBlacklist());
      this.whitelist = new HashSet<>(previousTable.getWhitelist());
      this.conditionalDrop = previousTable.getConditionalDrop();
    }
  }

  private void initTables() {
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

  void conditionallyDrop(EventSpec eventSpec) {
    if (conditionalDrop.add(eventSpec)) {
      blacklist.remove(eventSpec);
      whitelist.remove(eventSpec);
    }
  }

  boolean isBlacklisted(EventSpec eventSpec) {
    return blacklist.contains(eventSpec);
  }

  boolean isWhitelisted(EventSpec eventSpec) {
    return whitelist.contains(eventSpec);
  }

  boolean isConditionallyDropped(EventSpec eventSpec) {
    return conditionalDrop.contains(eventSpec);
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
