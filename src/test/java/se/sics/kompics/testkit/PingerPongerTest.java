package se.sics.kompics.testkit;

import org.junit.Test;
import se.sics.kompics.*;
import se.sics.kompics.testkit.fsm.FSM;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;

public class PingerPongerTest {

  private TestCase tc = TestKit.newTestCase(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component pinger2 = tc.create(Pinger.class, Init.NONE);
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private Component ponger2 = tc.create(Ponger.class, Init.NONE);
  private TestKit.Direction incoming = TestKit.Direction.INCOMING;
  private TestKit.Direction outgoing = TestKit.Direction.OUTGOING;

  @Test
  public void basicScenario() {

    tc.body().
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).

      expect(new Ping(), pinger.getNegative(PingPongPort.class), outgoing).
      expect(new Pong(new Ping()), pinger.getNegative(PingPongPort.class), incoming).
      repeat(1).body().
          trigger(new Pong(new Ping()), ponger.getPositive(PingPongPort.class).getPair()).
          expect(new Pong(new Ping()), pinger.getNegative(PingPongPort.class), incoming).
      end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void scratchTest(){
    tc.
      body().
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      //expect(new Ping(), pinger.getNegative(PingPongPort.class), outgoing).
      repeat(1).
        body().
        trigger(new Pong(new Ping()), ponger.getPositive(PingPongPort.class).getPair()).
        expect(new Pong(new Ping()), pinger.getNegative(PingPongPort.class), incoming).
      end().
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
    return !(o == null || !(o instanceof Ping));
  }
  public int hashCode() {
    return this.getClass().hashCode();
  }
}
  static class Pong extends Response {
    Pong(Request request) { super(request); }
    public boolean equals(Object o) {
      return !(o == null || !(o instanceof Pong));
    }

    public int hashCode() {
      return this.getClass().hashCode();
    }
  }
}
