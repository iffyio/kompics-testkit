package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;

import java.util.LinkedList;
import java.util.List;

class UnorderedSpec extends Spec{

  private final List<SingleEventSpec> expectUnordered;
  private List<SingleEventSpec> pending;
  private List<EventSpec> seen;

  UnorderedSpec(List<SingleEventSpec> expectUnordered) {
    this.expectUnordered = expectUnordered;
    seen = new LinkedList<EventSpec>();
    pending = new LinkedList<SingleEventSpec>(expectUnordered);
  }

  @Override
  StateTable.Transition getTransition(EventSpec receivedSpec, int state) {
    if (pending.contains(receivedSpec)) {
      int index = pending.indexOf(receivedSpec);
      seen.add(receivedSpec);
      pending.remove(index);

      int nextState = state;
      if (pending.isEmpty()) {
        for (EventSpec e : seen) {
          e.handle();
        }
        reset();
        nextState = state + 1;
      }
      // // TODO: 3/31/17 use more descriptive action for DROP here
      return new StateTable.Transition(receivedSpec, Action.DROP, nextState);
    }
    return null;
  }

  private void reset() {
    for (SingleEventSpec spec : expectUnordered) {
      pending.add(spec);
    }
    seen.clear();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("Unordered<Seen(");
    for (EventSpec e : seen) {
      sb.append(" ").append(e);
    }
    sb.append(")Pending(");
    for (Spec e : pending) {
      sb.append(" ").append(e);
    }
    sb.append(")>");
    return sb.toString();
  }

}
