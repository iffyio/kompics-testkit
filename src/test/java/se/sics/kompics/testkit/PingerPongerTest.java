package se.sics.kompics.testkit;

import com.google.common.base.Function;

import org.junit.Before;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
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

  private TestContext<Pinger> tc;
  private Component pinger;
  private Component ponger;
  private Negative<PingPongPort> pingerPort;
  private Positive<PingPongPort> pongerPort;

  private class Counter {
    int i = 0;
  }

  private Counter counter = new Counter();

  private BlockInit increment = new BlockInit() {
    @Override
    public void init() {
      counter.i++;
    }
  };

  @Test
  public void iterationInitTest() {

    int M = 11, N = 10;
    tc.
        connect(pingerPort, pongerPort).
        body().

        repeat(M).
        body().
            repeat(N, increment).
                onEachIteration(increment).
            body()
                .trigger(new Ping(0), pingerPort.getPair())
                .expect(new Ping(0), pingerPort, OUTGOING)
            .end()
        .end();

    assert tc.check_();
    assertEquals(counter.i, M * N + M);
  }

  @Test
  public void initOnMultipleBlocksTest() {

    int M = 10, N = 12;
    tc.
        connect(pingerPort, pongerPort).
        body().

        repeat(M, increment).
        body().
            repeat(N, increment).
                onEachIteration(increment).
            body()
                .trigger(new Ping(0), pingerPort.getPair())
                .expect(new Ping(0), pingerPort, OUTGOING)
            .end()
        .end();

    assert tc.check_();
    assertEquals(counter.i, M * N + M + M);
  }

  @Test
  public void defaultActionTest() {
    tc.setDefaultAction(Ping.class, new Function<Ping, Action>() {
        @Override
        public Action apply(Ping ping) {
          if (ping.count < 3) {
            return Action.HANDLE;
          }
          return Action.FAIL;
        }
    });

    tc.setDefaultAction(KompicsEvent.class, new Function<KompicsEvent, Action>() {
        @Override
        public Action apply(KompicsEvent event) {
          return Action.FAIL;
        }
    });

    int M = 3, N = 30;
    tc.connect(pingerPort, pongerPort).
      body().

        repeat(M).
        body().
          repeat(N, increment).
            onEachIteration(increment).
          body()
            .trigger(new Ping(0), pingerPort.getPair())
            .trigger(new Ping(0), pingerPort.getPair())
            .trigger(new Ping(0), pingerPort.getPair())
            .trigger(new Ping(0), pingerPort.getPair())
            .trigger(new Ping(1), pingerPort.getPair())
            .expect(new Ping(1), pingerPort, OUTGOING)
          .end()
        .end();

    assert tc.check_();
    assertEquals(counter.i, M * N + M);
  }

  @Before
  public void init() {
    tc = Testkit.newTestContext(Pinger.class, Init.NONE);
    pinger = tc.getComponentUnderTest();
    ponger = tc.create(Ponger.class, Init.NONE);
    pingerPort = pinger.getNegative(PingPongPort.class);
    pongerPort = ponger.getPositive(PingPongPort.class);
  }

  public static class Pinger extends ComponentDefinition {
    static int counter = 0;

    Positive<PingPongPort> ppPort = requires(PingPongPort.class);

    Handler<Pong> pongHandler = new Handler<Pong>() {
      @Override
      public void handle(Pong event) {
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) {
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
      public void handle(Ping ping) { }
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
