package se.sics.kompics.testkit.fsm;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.testkit.Direction;
import se.sics.kompics.testkit.ProxyHandler;

import java.util.Comparator;

public class EventSpec<E extends KompicsEvent> implements Spec{

  private final E event;
  private final Port<? extends PortType> port;
  private final Direction direction;
  private Comparator<E> comparator;
  private ProxyHandler handler;

  EventSpec(E event, Port<? extends PortType> port,
            Direction direction, Comparator<E> comparator) {
    this(event, port, direction);
    this.comparator = comparator;
  }

  // // TODO: 2/17/17 remove constructor if not needed
  public EventSpec(E event, Port<? extends PortType> port,
            Direction direction) {
    this.event = event;
    this.port = port;
    this.direction = direction;
  }

  public E getEvent() {
    return event;
  }

  public Port<? extends PortType> getPort() {
    return port;
  }

  public Direction getDirection() {
    return direction;
  }

  public void setHandler(ProxyHandler handler) {
    this.handler = handler;
  }

  void setComparator(Comparator<E> comparator) {
    this.comparator = comparator;
  }

  public void handle() {
    handler.doHandle(event);
  }

  public boolean equals(Object o) {
    if (o == null || !(o instanceof EventSpec)) {
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
