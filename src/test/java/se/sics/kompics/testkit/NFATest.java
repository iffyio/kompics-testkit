package se.sics.kompics.testkit;

import org.junit.Before;
import org.junit.Test;

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
import se.sics.kompics.testkit.urb.Counter;

import static se.sics.kompics.testkit.Direction.INCOMING;
import static se.sics.kompics.testkit.Direction.OUTGOING;

public class NFATest {

  private TestContext<Pinger> tc;
  private Component pinger;
  private Component ponger;
  private Negative<PingPongPort> pingerPort;
  private Positive<PingPongPort> pongerPort;

  @Before
  public void init() {
    tc = Testkit.newTestContext(Pinger.class, Init.NONE);
    pinger = tc.getComponentUnderTest();
    ponger = tc.create(Ponger.class, Init.NONE);
    pingerPort = pinger.getNegative(PingPongPort.class);
    pongerPort = ponger.getPositive(PingPongPort.class);
    tc.connect(pingerPort, pongerPort);
  }

  @Test
  public void basicTest() {
    tc.body()
        .trigger(ping(0), pingerPort.getPair())
        .trigger(ping(1), pingerPort.getPair())
        .trigger(ping(2), pingerPort.getPair())
        .expect(ping(0), pingerPort, OUTGOING)
        .expect(ping(1), pingerPort, OUTGOING)
        .expect(ping(2), pingerPort, OUTGOING);
    assert tc.check_();
  }

  Counter counter = new Counter();
  @Test
  public void basicRepeatTest() {
    tc.body()
        .trigger(pong(0), pongerPort.getPair())
        .expect(pong(0), pingerPort, INCOMING)
        .repeat(2).body()
            .trigger(pong(1), pongerPort.getPair())
            .expect(pong(1), pingerPort, INCOMING)
        .end()
        .trigger(pong(0), pongerPort.getPair())
        .expect(pong(0), pingerPort, INCOMING)
        .repeat(3).body()
            .trigger(pong(1), pongerPort.getPair())
            .expect(pong(1), pingerPort, INCOMING)
        .end()
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .repeat(3).body()
            .expect(pong(1), pingerPort, INCOMING)
        .end()
    ;
    assert tc.check_();
    assert Pinger.counter == 10;
  }

  BlockInit increment = new BlockInit() {
    @Override
    public void init() {
      counter.i++;
    }
  };

  @Test
  public void kleeneStarAmbiguousTest() {
    tc.body()
        .repeat(increment)
            .onEachIteration(increment)
        .body()
        .end()

        .trigger(ping(0), pingerPort.getPair())
        .expect(ping(0), pingerPort, OUTGOING)
    ;

    assert tc.check_();
  }

  @Test
  public void kleeneStarTest() {
    tc
        .blockExpect(ping(3), pingerPort, OUTGOING)
        .blockExpect(ping(4), pingerPort, OUTGOING)

    .body()
        .trigger(ping(0), pingerPort.getPair())
        .trigger(ping(0), pingerPort.getPair())
        .trigger(ping(0), pingerPort.getPair())
        .trigger(ping(4), pingerPort.getPair())
        .trigger(ping(1), pingerPort.getPair())

        .repeat()
        .body()
            .expect(ping(0), pingerPort, OUTGOING)
        .end()

        .expect(ping(1), pingerPort, OUTGOING)
        .trigger(ping(3), pingerPort.getPair())
        //.expect(ping(3), pingerPort, OUTGOING)
    ;

    assert tc.check_();
  }

  @Test
  public void allowDisallowDropTest() {
    int N = 4, M = 3;
    tc
        .allow(pong(1), pingerPort, INCOMING)
        .disallow(pong(0), pingerPort, INCOMING)
        .drop(pong(2), pingerPort, INCOMING)
        .body()
            .repeat(M).body()
                .trigger(pong(2), pongerPort.getPair())
                .trigger(pong(0), pongerPort.getPair())
                .trigger(pong(1), pongerPort.getPair())
            .end()

            .repeat(N).body()
                .trigger(pong(3), pongerPort.getPair())
            .end()

            .trigger(pong(1), pongerPort.getPair())
            .trigger(pong(2), pongerPort.getPair())
            .trigger(pong(2), pongerPort.getPair())
            .trigger(pong(3), pongerPort.getPair())

            .repeat(N)
                .allow(pong(0), pingerPort, INCOMING)
                .allow(pong(2), pingerPort, INCOMING)
            .body()
                .expect(pong(3), pingerPort, INCOMING)
            .end()

            .expect(pong(3), pingerPort, INCOMING)
    ;

    assert tc.check_();
    assert Pinger.counter == 3 * M + N + 2;
  }

  @Test
  public void unorderedTest1() {
    tc.body()
        .trigger(ping(2), pingerPort.getPair())
        .trigger(ping(1), pingerPort.getPair())
        .trigger(ping(3), pingerPort.getPair())
    ;
    unorderedTest();
  }

  @Test
  public void unorderedTest2() {
    tc.body()
        .trigger(ping(0), pingerPort.getPair())
        .expect(ping(0), pingerPort, OUTGOING)

        .trigger(ping(3), pingerPort.getPair())
        .trigger(ping(2), pingerPort.getPair())
        .trigger(ping(1), pingerPort.getPair())
    ;
    unorderedTest();
  }

  private void unorderedTest() {
    tc
        .unordered()
            .expect(ping(1), pingerPort, OUTGOING)
            .expect(ping(2), pingerPort, OUTGOING)
            .expect(ping(3), pingerPort, OUTGOING)
        .end()

        .trigger(ping(0), pingerPort.getPair())
        .expect(ping(0), pingerPort, OUTGOING)

    ;

    assert tc.check_();
  }

  private Ping ping(int count) {
    return new Ping(count);
  }

  private Pong pong(int count) {
    return new Pong(count);
  }

  public static class Pinger extends ComponentDefinition {
    Positive<PingPongPort> ppPort = requires(PingPongPort.class);
    static int counter = 0;

    Handler<Pong> pongHandler = new Handler<Pong>() {
      @Override
      public void handle(Pong event) {
        //trigger(new Ping(++counter), ppPort);
        counter++;
      }
    };

    {
      subscribe(pongHandler, ppPort);
    }
  }

  public static class Ponger extends ComponentDefinition {
    static int counter = 0;

    { provides(PingPongPort.class); }
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
      return "Ping " + count;
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
      return "Pong " + count;
    }
  }
}
