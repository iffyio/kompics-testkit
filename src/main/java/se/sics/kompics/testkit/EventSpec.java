package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

import java.util.Comparator;

class EventSpec extends SingleEventSpec{

  private final KompicsEvent event;
  private final Comparator<? extends KompicsEvent> comparator;
  private ProxyHandler handler;

  <E extends KompicsEvent> EventSpec(E event, Port<? extends PortType> port,
            Direction direction, Comparator<E> comparator) {
    super(port, direction);
    this.event = event;
    this.comparator = comparator;
  }

  static <P extends  PortType, E extends KompicsEvent> EventSpec create(
          Comparator<E> comparator, E event, Port<P> port, Direction direction) {
    return new EventSpec(event, port, direction, comparator);
  }

  KompicsEvent getEvent() {
    return event;
  }

  Port<? extends PortType> getPort() {
    return port;
  }

  Direction getDirection() {
    return direction;
  }

  void setHandler(ProxyHandler handler) {
    this.handler = handler;
  }

  void handle() {
    handler.doHandle(event);
  }

  @Override
  public boolean match(EventSpec receivedSpec) {
    return this.equals(receivedSpec);
  }

  public boolean equals(Object o) {
    if (o instanceof EventSpec) {
      return equalEventSpec((EventSpec) o);
    }
    if (o instanceof PredicateSpec) {
      return equalPredicateSpec((PredicateSpec) o);
    }

    return false;
  }

  private boolean equalPredicateSpec(PredicateSpec predicateSpec) {
    return predicateSpec.match(this) &&
           port.equals(predicateSpec.port) &&
           direction.equals(predicateSpec.direction);
  }

  private boolean equalEventSpec(EventSpec other) {
    KompicsEvent e = other.getEvent();

    return e.getClass() == event.getClass() &&
           port.equals(other.getPort()) &&
           direction.equals(other.getDirection()) &&
           comparator == null? event.equals(e) : equalByComparator(comparator, e);
  }

  private <V extends KompicsEvent> boolean equalByComparator(Comparator<V> comp, KompicsEvent e) {
    return event.getClass() == e.getClass() && comp.compare((V) event, (V) e) == 0;
  }

  @Override
  public int hashCode() {
    // event is excluded since user defines it's equality
    int result = 31 * port.hashCode();
    result = 31 * result + direction.hashCode();
    return result;
  }

  public String toString() {
    return direction + " " + event;
  }
}
