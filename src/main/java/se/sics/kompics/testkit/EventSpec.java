package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

import java.util.Comparator;

class EventSpec<E extends KompicsEvent> extends Spec{

  private final E event;
  private Comparator<E> comparator;
  private ProxyHandler handler;

  private EventSpec(E event, Port<? extends PortType> port,
            Direction direction, Comparator<E> comparator) {
    super(port, direction);
    this.event = event;
    this.comparator = comparator;
  }

  static <P extends  PortType, E extends KompicsEvent> EventSpec<? extends KompicsEvent> create(
          Comparator<E> comparator, E event, Port<P> port, Direction direction) {
    return new EventSpec<E>((E) event, port, direction, comparator);
  }

  E getEvent() {
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

  void setComparator(Comparator<E> comparator) {
    this.comparator = comparator;
  }

  void handle() {
    handler.doHandle(event);
  }

  public boolean match(EventSpec<? extends KompicsEvent> receivedSpec) {
    return this.equals(receivedSpec);
  }

  public boolean equals(Object o) {
    if (!(o instanceof EventSpec)) {
      return false;
    }
    EventSpec other = (EventSpec) o;
    KompicsEvent e = other.getEvent();

    if (!e.getClass().equals(event.getClass())) {
      return false;
    }

    return port.equals(other.getPort()) &&
           direction.equals(other.getDirection()) &&
           comparator == null? event.equals(e) :
           comparator.compare(event, (E) e) == 0;
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
