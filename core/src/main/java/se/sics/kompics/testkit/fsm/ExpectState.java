package se.sics.kompics.testkit.fsm;

import se.sics.kompics.*;
import se.sics.kompics.testkit.TestKit;

public class ExpectState extends State{

  private final FSM fsm;
  private final KompicsEvent event;
  private final Port<? extends PortType> port;
  private final TestKit.Direction direction;

  public ExpectState(
          FSM fsm, KompicsEvent event,
          Port<? extends PortType> port, TestKit.Direction direction) {

    this.fsm = fsm;
    this.event = event;
    this.port = port;
    this.direction = direction;
  }

  @Override
  protected boolean run() {
    // check queue
    KompicsEvent queuedEvent = fsm.peekEventQueue();
    if (queuedEvent == null) {
      Kompics.logger.info("expect: Empty queue, Expecting {}", event);
      fsm.waitNewEvent();
      return false;
    } else if (!queuedEvent.equals(event)) {
      Kompics.logger.info("expect: Expecting {}, received {}", event, queuedEvent);
      fsm.waitNewEvent();
      return false;
    } else {
      fsm.pollEventQueue();
      return true;
    }
  }

}
