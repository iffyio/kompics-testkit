package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

class PredicateSpec implements SingleEventSpec{

  final Class<? extends KompicsEvent> eventType;
  final Predicate<? extends KompicsEvent> predicate;
  final Port<? extends PortType> port;
  final Direction direction;

  <E extends KompicsEvent> PredicateSpec(
          Class<E> eventType, Predicate<E> predicate,
          Port<? extends PortType> port, Direction direction) {
    this.port = port;
    this.direction = direction;
    this.eventType = eventType;
    this.predicate = predicate;
  }

  @Override
  public boolean match(EventSpec receivedSpec) {
    KompicsEvent receivedEvent = receivedSpec.getEvent();
    return eventType.equals(receivedEvent.getClass()) &&
           matchHelper(predicate, receivedEvent);
  }

  private <E extends KompicsEvent> boolean matchHelper(Predicate<E> predicate, KompicsEvent receivedEvent) {
    E r = (E) receivedEvent;
    return predicate.apply(r);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof EventSpec)) { // only comparable with EventSpec (lookups for received events)
      return false;
    }
    EventSpec eventSpec = (EventSpec) o;

    return match(eventSpec) &&
           port.equals(eventSpec.getPort()) &&
           direction.equals(eventSpec.getDirection());
  }

  @Override
  public int hashCode() {
    int result = 31 * port.hashCode();
    result = 31 * result + direction.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "( " + direction + " " + predicate.toString() + " )";
  }
}
