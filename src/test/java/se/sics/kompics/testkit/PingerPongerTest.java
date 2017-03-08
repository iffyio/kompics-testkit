package se.sics.kompics.testkit;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;
import se.sics.kompics.*;

import java.util.*;

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
  public void defaultHandlerTest() {
    List<Class<? extends KompicsEvent>> list = new ArrayList<>(Arrays.asList(
            KompicsEvent.class,Ping.class, Ping.class, Pong.class, Sping.class, Spong.class
    ));

    Comparator<Class<? extends KompicsEvent>> classComparator = new Comparator<Class<? extends KompicsEvent>>() {
      @Override
      public int compare(Class<? extends KompicsEvent> e1, Class<? extends KompicsEvent> e2) {
        if (e1 == e2) {
          return 0;
        } else if (e1.isAssignableFrom(e2)) {
          return 1;
        } else {
          return -1;
        }
      }
    };

    TreeSet<Class<? extends KompicsEvent>> ts = new TreeSet<Class<? extends KompicsEvent>>(classComparator);
    for (Class<? extends KompicsEvent> c : list) ts.add(c);

    for (Class<? extends KompicsEvent> c : ts)
      Kompics.logger.warn("{}", c.getSimpleName());
  }
  class Sping extends Ping {
    Sping(int count) {
      super(count);
    }
  }
  class Spong extends Pong {
    Spong(int count) {
      super(count);
    }
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
