package se.sics.kompics.testkit.fsm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class Environment {
  private Set<EventSpec> disallowed;
  private Set<EventSpec> allowed;
  private Set<EventSpec> dropped;

  Environment(Environment previousEnv) {
    if (previousEnv == null) {
      initEmptyBlock();
    } else {
      this.disallowed = new HashSet<>(previousEnv.disallowed);
      this.allowed = new HashSet<>(previousEnv.allowed);
      this.dropped = new HashSet<>(previousEnv.dropped);
    }
  }

  private void initEmptyBlock() {
    disallowed = new HashSet<>();
    allowed = new HashSet<>();
    dropped = new HashSet<>();
  }

  void addDisallowedMessage(EventSpec eventSpec) {
    if (disallowed.add(eventSpec)) {
      allowed.remove(eventSpec);
      dropped.remove(eventSpec);
    }
  }

  void addAllowedMessage(EventSpec eventSpec) {
    if (allowed.add(eventSpec)) {
      disallowed.remove(eventSpec);
      dropped.remove(eventSpec);
    }
  }

  void addDroppedMessage(EventSpec eventSpec) {
    if (dropped.add(eventSpec)) {
      disallowed.remove(eventSpec);
      allowed.remove(eventSpec);
    }
  }

  Collection<EventSpec> getDisallowedEvents() {
    return disallowed;
  }
  Collection<EventSpec> getAllowedEvents() {
    return allowed;
  }
  Collection<EventSpec> getDroppedEvents() {
    return dropped;
  }

}
