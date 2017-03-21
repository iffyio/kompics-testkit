package se.sics.kompics.testkit.fsm;

import se.sics.kompics.KompicsEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class Environment {
  private Set<EventSpec<? extends KompicsEvent>> disallowed;
  private Set<EventSpec<? extends KompicsEvent>> allowed;
  private Set<EventSpec<? extends KompicsEvent>> dropped;

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

  void addDisallowedMessage(EventSpec<? extends KompicsEvent> eventSpec) {
    if (disallowed.add(eventSpec)) {
      allowed.remove(eventSpec);
      dropped.remove(eventSpec);
    }
  }

  void addAllowedMessage(EventSpec<? extends KompicsEvent> eventSpec) {
    if (allowed.add(eventSpec)) {
      disallowed.remove(eventSpec);
      dropped.remove(eventSpec);
    }
  }

  void addDroppedMessage(EventSpec<? extends KompicsEvent> eventSpec) {
    if (dropped.add(eventSpec)) {
      disallowed.remove(eventSpec);
      allowed.remove(eventSpec);
    }
  }

  Collection<EventSpec<? extends KompicsEvent>> getDisallowedEvents() {
    return disallowed;
  }
  Collection<EventSpec<? extends KompicsEvent>> getAllowedEvents() {
    return allowed;
  }
  Collection<EventSpec<? extends KompicsEvent>> getDroppedEvents() {
    return dropped;
  }

}
