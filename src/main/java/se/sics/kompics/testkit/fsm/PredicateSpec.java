package se.sics.kompics.testkit.fsm;

import com.google.common.base.Predicate;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.testkit.TestKit;

// // TODO: 2/21/17 merge with event spec
class PredicateSpec implements Spec{

  private final Port<? extends PortType> port;
  private final TestKit.Direction direction;
  private final Class<? extends KompicsEvent> eventType;
  private final Predicate<? extends KompicsEvent> predicate;

  <E extends KompicsEvent> PredicateSpec(
          Class<E> eventType, Predicate<E> predicate,
          Port<? extends PortType> port, TestKit.Direction direction) {
    this.eventType = eventType;
    this.predicate = predicate;
    this.port = port;
    this.direction = direction;
  }

  Predicate getPredicate() {
    return predicate;
  }

  public boolean equals(Object o) {
    if (o == null || !(o instanceof PredicateSpec)) {
      return false;
    }
    PredicateSpec other = (PredicateSpec) o;

    return eventType.equals(other.eventType) &&
            port.equals(other.port) &&
            direction.equals(other.direction);
  }

  @Override
  public int hashCode() {
    int result = 31 * port.hashCode();
    result = 31 * result + direction.hashCode();
    result = 31 * result + eventType.hashCode();
    return result;
  }

  public String toString() {
    return "( " + direction + " " + predicate.toString() + " )";
  }
}
