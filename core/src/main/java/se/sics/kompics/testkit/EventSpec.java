package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

public class EventSpec {

  private final KompicsEvent event;
  private final Port<? extends PortType> port;
  private final TestKit.Direction direction;
  private TestHandler handler;

  EventSpec(KompicsEvent event, Port<? extends PortType> port,
                   TestKit.Direction direction) {
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

  void setHandler(TestHandler handler) {
    this.handler = handler;
  }

  public void handle() {
    handler.doHandle(event);
  }

  public boolean equals(Object o) {
    if (o == null || !(o instanceof EventSpec)) {
      return false;
    }
    EventSpec other = (EventSpec) o;
    return event.equals(other.getEvent()) &&
            port.equals(other.getPort()) &&
            direction.equals(other.getDirection());
  }

  @Override
  public int hashCode() {
    int result = event.hashCode();
    result = 31 * result + port.hashCode();
    result = 31 * result + direction.hashCode();
    return result;
  }

  public String toString() {
    return direction + " event " + event + " on port " + port;
  }
}
