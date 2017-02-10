package se.sics.kompics.testkit.fsm;

import se.sics.kompics.*;
import se.sics.kompics.testkit.TestKit;

public class ExpectState extends State{

  private final FSM fsm;
  private final QueuedEvent expectedEvent;

  public ExpectState(
          FSM fsm, KompicsEvent event,
          Port<? extends PortType> port, TestKit.Direction direction) {

    this.fsm = fsm;
    expectedEvent = new QueuedEvent(event, port, direction);
  }

  @Override
  protected boolean run() {
    QueuedEvent  queuedEvent = fsm.pollEventQueue();
    assert queuedEvent != null;

    if (expectedEvent.equals(queuedEvent)) {
      return true;
    } else {
      Kompics.logger.info("expect: Expected {}\nreceived {}", expectedEvent, queuedEvent);
      return false;
    }
  }

}
