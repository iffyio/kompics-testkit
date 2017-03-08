package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;
import se.sics.kompics.*;

public class PingerPongerTest {

  private TestContext<Pinger> tc = Testkit.newTestContext(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private Direction incoming = Direction.INCOMING;
  private Direction outgoing = Direction.OUTGOING;

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
          expect(ping, pinger.getNegative(PingPongPort.class), outgoing).
          expect(pong, pinger.getNegative(PingPongPort.class), incoming).
        end().
      end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void defaultActionTest() {
    tc.
      setDefaultAction(Pong.class, new Predicate<Pong>() {
        @Override
        public boolean apply(Pong event) {
          return true;
        }
      }).
      setDefaultAction(KompicsEvent.class, new Predicate<KompicsEvent>() {
        @Override
        public boolean apply(KompicsEvent event) {
          return false;
        }
      }).
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      body().

        repeat(3).
        body().
          repeat(30, resetPong).
            onEachIteration(incrementCounters).
          body().
            expect(ping, pinger.getNegative(PingPongPort.class), outgoing).
          end().
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
