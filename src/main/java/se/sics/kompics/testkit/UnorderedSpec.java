package se.sics.kompics.testkit;

import java.util.LinkedList;
import java.util.List;

class UnorderedSpec implements MultiEventSpec{

  private final List<SingleEventSpec> expectUnordered;
  private List<SingleEventSpec> pending;
  private List<EventSpec> seen;

  UnorderedSpec(List<SingleEventSpec> expectUnordered) {
    this.expectUnordered = expectUnordered;
    seen = new LinkedList<EventSpec>();
    pending = new LinkedList<SingleEventSpec>(expectUnordered);
  }

  @Override
  public boolean match(EventSpec receivedSpec) {
    if (pending.contains(receivedSpec)) {
      int index = pending.indexOf(receivedSpec);
      seen.add(receivedSpec);
      pending.remove(index);

      if (pending.isEmpty()) {
        for (EventSpec e : seen) {
          e.handle();
        }
        reset();
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean isComplete() {
    return seen.isEmpty();
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
