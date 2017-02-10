package se.sics.kompics.testkit.fsm;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.testkit.TestKit;

public class QueuedEvent {

  private final KompicsEvent event;
  private final Port<? extends PortType> port;
  private final TestKit.Direction direction;

  public QueuedEvent(KompicsEvent event, Port<? extends PortType> port, TestKit.Direction direction) {
    this.event = event;
    this.port = port;
    this.direction = direction;
  }

  public KompicsEvent getEvent() {
    return event;
  }

  public Port<? extends PortType> getPort() {
    return port;
  }

  public TestKit.Direction getDirection() {
    return direction;
  }

  public boolean equals(Object o) {
    if (o == null || !(o instanceof QueuedEvent)) {
      return false;
    }
    QueuedEvent other = (QueuedEvent) o;
    return event.equals(other.getEvent()) &&
            port.equals(other.getPort()) &&
            direction.equals(other.getDirection());
  }
  public String toString() {
    return direction + " event " + event + " on port " + port;
  }
}
