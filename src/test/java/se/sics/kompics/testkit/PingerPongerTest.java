package se.sics.kompics.testkit;

import org.junit.Test;
import se.sics.kompics.*;

public class PingerPongerTest {

  @Test
  public void run() {
    TestCase tc = TestKit.newTestCase(Pinger.class, Init.NONE);
    Component pinger = tc.getComponentUnderTest();
    Component pinger2 = tc.create(Pinger.class, Init.NONE);
    Component ponger = tc.create(Ponger.class, Init.NONE);
    Component ponger2 = tc.create(Ponger.class, Init.NONE);
    TestKit.Direction incoming = TestKit.Direction.INCOMING;
    TestKit.Direction outgoing = TestKit.Direction.OUTGOING;

    tc.
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).

      expect(new Ping(), pinger.getNegative(PingPongPort.class), outgoing).
      expect(new Pong(new Ping()), pinger.getNegative(PingPongPort.class), incoming).
      //conditionalDrop(new Pong(new Ping()), pinger.getNegative(PingPongPort.class), incoming).
      repeat(1).
          trigger(new Pong(new Ping()), ponger.getPositive(PingPongPort.class).getPair()).
/*            repeat(4).
              trigger(new Pong(new Ping()), ponger.getPositive(PingPongPort.class).getPair()).
              expect(new Pong(new Ping()), pinger.getNegative(PingPongPort.class), incoming).
            endRepeat().*/
            //repeat(3).endRepeat().
          expect(new Pong(new Ping()), pinger.getNegative(PingPongPort.class), incoming).
      endRepeat().
      check();
    Kompics.logger.info("Done!");
  }
  /*
  does order/scope matter for conditionals
  do response msgs work in other direction
  support for direct.request
  Failure detector scenario
   */

  private void testCase1(TestCase tc, Component pinger, Component ponger,
                         TestKit.Direction incoming, TestKit.Direction outgoing) {
    tc.
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      expect(new Ping(), pinger.getNegative(PingPongPort.class), outgoing).
      repeat(1).
        trigger(new Pong(new Ping()), ponger.getPositive(PingPongPort.class).getPair()).
        expect(new Pong(new Ping()), pinger.getNegative(PingPongPort.class), incoming).
      endRepeat().
    check();
  }

  // COMPONENTS

  public static class Parent extends ComponentDefinition {
    Component pinger = create(Pinger.class, Init.NONE);
    Component pinger2 = create(Pinger.class, Init.NONE);
    Component ponger = create(Ponger.class, Init.NONE);
    {
      connect(pinger.getNegative(PingPongPort.class),
              ponger.getPositive(PingPongPort.class));
      connect(pinger.getNegative(PingPongPort.class).getPair(),
              pinger2.getNegative(PingPongPort.class));
    }
  }

  public static class Pinger extends ComponentDefinition {
    public Pinger() {}
    Positive<PingPongPort> ppPort = requires(PingPongPort.class);
    static int k = 0;
    int i = 0;
    Handler<Pong> pongHandler = new Handler<Pong>() {
      @Override
      public void handle(Pong event) {
        Kompics.logger.error("pinger: Received Pong! {}", event);
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) {
        if (k++ <= 1) {
          Kompics.logger.info("pinger {}: ", getComponentCore().getComponent().id());
          Kompics.logger.info("parent {}: ", getComponentCore().getParent().getComponent().id());
          trigger(new Ping(), ppPort);
        } else {
          Kompics.logger.info("pinger2 {}: ", getComponentCore().getComponent().id());
        }
      }
    };

    {
      subscribe(pongHandler, ppPort);
      subscribe(startHandler, control);
    }
  }

  public static class Ponger extends ComponentDefinition {
    Negative<PingPongPort> pingPongPort = provides(PingPongPort.class);

    Handler<Ping> pingHandler = new Handler<Ping>() {
      @Override
      public void handle(Ping ping) {
        //Kompics.logger.info("Ponger: received {}", ping);
        trigger(new Pong(ping), pingPongPort);
        //answer(event, new Pong());
      }
    };

    {
      subscribe(pingHandler, pingPongPort);
    }
  }

  public static class PingPongPort extends PortType {
    {
      request(Ping.class);
      indication(Pong.class);
      //indication(SuperPong.class);
    }
  }

/*  static class Ping implements KompicsEvent{}
  static class SuperPong extends Pong{}
  static class Pong implements KompicsEvent{}*/
  static class Ping extends Request {
  public boolean equals(Object o) {
    if (o == null || !(o instanceof Ping)) {
      return false;
    }
    return true;
  }
  public int hashCode() {
    return this.getClass().hashCode();
  }
}
  static class Pong extends Response {
    Pong(Request request) { super(request); }
    public boolean equals(Object o) {
      if (o == null || !(o instanceof Pong)) {
        return false;
      }
      return true;
    }

    public int hashCode() {
      return this.getClass().hashCode();
    }
  }
}
