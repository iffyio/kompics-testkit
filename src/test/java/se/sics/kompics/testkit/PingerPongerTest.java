package se.sics.kompics.testkit;

import com.google.common.base.Function;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.Request;
import se.sics.kompics.Start;

import static se.sics.kompics.testkit.Direction.INCOMING;
import static se.sics.kompics.testkit.Direction.OUTGOING;

public class PingerPongerTest {

  private TestContext<Pinger> tc = Testkit.newTestContext(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component ponger = tc.create(Ponger.class, Init.NONE);

  private Ping ping = new Ping(0);
  private Pong pong = new Pong(0);
  private LoopInit resetPong = new LoopInit() {
    @Override
    public void init() {
      pong.count = 0;
      Ponger.counter = 0;
    }
  };

  private LoopInit incrementCounters = new LoopInit() {
    @Override
    public void init() {
      ping.count++;
      pong.count++;
    }
  };

  @Test
  public void iterationInitTest() {

    tc.
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      body().

      repeat(3).
      body().
        repeat(3, resetPong).
          onEachIteration(incrementCounters).
        body().
          expect(ping, pinger.getNegative(PingPongPort.class), OUTGOING).
          expect(pong, pinger.getNegative(PingPongPort.class), INCOMING).
        end().
      end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void defaultActionTest() {
    tc.
      setDefaultAction(Pong.class, new Function<Pong, Action>() {
        @Override
        public Action apply(Pong event) {
          return Action.HANDLE;
        }
      }).
      setDefaultAction(KompicsEvent.class, new Function<KompicsEvent, Action>() {
        @Override
        public Action apply(KompicsEvent event) {
          return Action.FAIL;
        }
      }).
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      body().

        repeat(3).
        body().
          repeat(30, resetPong).
            onEachIteration(incrementCounters).
          body().
            expect(ping, pinger.getNegative(PingPongPort.class), OUTGOING).
          end().
        end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  private static int MAGIC_NUMBER = 7;
  @Test
  public void assertThrownTest() {
    tc.
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      body().

      repeat(10).
      body().
        repeat(MAGIC_NUMBER - 1, resetPong).
          onEachIteration(incrementCounters).
        body().
          expect(ping, pinger.getNegative(PingPongPort.class), OUTGOING).
          expect(pong, pinger.getNegative(PingPongPort.class), INCOMING).
        end().

            // assert error thrown on nth pong
        expect(ping, pinger.getNegative(PingPongPort.class), OUTGOING).
        expect(pong, pinger.getNegative(PingPongPort.class), INCOMING).
        assertThrown(IllegalStateException.class, Fault.ResolveAction.DESTROY).
      end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  public static class Pinger extends ComponentDefinition {
    static int counter = 0;

    Positive<PingPongPort> ppPort = requires(PingPongPort.class);

    Handler<Pong> pongHandler = new Handler<Pong>() {
      @Override
      public void handle(Pong event) {
        trigger(new Ping(++counter), ppPort);
        if (counter % MAGIC_NUMBER == 0) {
          throw new IllegalStateException("multiple of " + MAGIC_NUMBER);
        }
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) {
          trigger(new Ping(++counter), ppPort);
      }
    };

    {
      subscribe(pongHandler, ppPort);
      subscribe(startHandler, control);
    }
  }

  public static class Ponger extends ComponentDefinition {
    static int counter = 0;

    Negative<PingPongPort> pingPongPort = provides(PingPongPort.class);

    Handler<Ping> pingHandler = new Handler<Ping>() {
      @Override
      public void handle(Ping ping) {
        Pong pong = new Pong(++counter);
        trigger(pong, pingPongPort);
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
    }
  }

  static class Ping extends Request {
    int count;
    Ping(int count) { this.count = count; }

    public boolean equals(Object o) {
      return o instanceof Ping && ((Ping) o).count == count;
    }

    public int hashCode() {
      return count;
    }

    public String toString() {
      return "" + count;
    }
  }

  static class Pong implements KompicsEvent {

    int count;
    Pong(int count) { this.count = count; }

    public boolean equals(Object o) {
      return o instanceof Pong && ((Pong)o).count == count;
    }

    public int hashCode() {
      return count;
    }

    public String toString() {
      return "" + count;
    }
  }
}
