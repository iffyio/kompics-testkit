package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import org.junit.Test;
import se.sics.kompics.*;

import static junit.framework.Assert.assertEquals;

public class InspectTest {

  private TestContext<Pinger> tc = Testkit.newTestContext(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private Direction incoming = Direction.INCOMING;
  private Direction outgoing = Direction.OUTGOING;
  private static Ping ping = new Ping();
  private static Pong pong = new Pong();

  @Test
  public void work() {
    tc.connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class));
    tc.body().
       repeat(10).body().
        expect(ping, pinger.getNegative(PingPongPort.class), outgoing).
        inspect(expectedPings).
        expect(pong, pinger.getNegative(PingPongPort.class), incoming).
        inspect(expectedPongs).
       end();

    //assertEquals(tc.check(), tc.getFinalState());
    assert tc.check_();
  }

  private Predicate<Pinger> expectedPings = new Predicate<Pinger>() {
    int expectedPingsSent = 0;
    @Override
    public boolean apply(Pinger pinger) {
      return pinger.pingsSent == ++expectedPingsSent;
    }
  };

  private Predicate<Pinger> expectedPongs = new Predicate<Pinger>() {
    int expectedPongsReceived = 0;
    @Override
    public boolean apply(Pinger pinger) {
      return pinger.pongsReceived == ++expectedPongsReceived;
    }
  };

  public static class Pinger extends ComponentDefinition {
    Positive<PingPongPort> ppPort = requires(PingPongPort.class);
    int pingsSent = 0;
    int pongsReceived = 0;

    Handler<Pong> pongHandler = new Handler<Pong>() {
      @Override
      public void handle(Pong pong) {
        trigger(ping, ppPort);
        pingsSent++;
        pongsReceived++;
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) {
        trigger(ping, ppPort);
        pingsSent++;
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

  public static class Ping implements KompicsEvent{ }
  public static class Pong implements KompicsEvent{ }
}
