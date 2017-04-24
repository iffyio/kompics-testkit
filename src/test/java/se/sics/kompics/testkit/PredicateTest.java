package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import org.junit.Test;
import se.sics.kompics.*;
import java.util.Comparator;

import static junit.framework.Assert.assertEquals;

public class PredicateTest {

  private TestContext<Pinger> tc = Testkit.newTestContext(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private Direction incoming = Direction.INCOMING;
  private Direction outgoing = Direction.OUTGOING;

  @Test
  public void mixComparatorsAndPredicates() {
    Negative pingerPort = pinger.getNegative(PingPongPort.class);
    Positive pongerPort = ponger.getPositive(PingPongPort.class);

    Ping allowedPing = ping(10);
    tc.connect(pingerPort, pongerPort);
    tc.
      addComparator(Pong.class, new PongComparator()).
      addComparator(Ping.class, new PingComparator()).
      //disallow(new Pong(1), pingerPort, incoming).
      allow(allowedPing, pingerPort, outgoing).

      body()
          .trigger(ping(0), pingerPort.getPair())
          .trigger(allowedPing, pingerPort.getPair())
          .trigger(pong(0), pongerPort.getPair())

          .trigger(ping(1), pingerPort.getPair())
          .trigger(allowedPing, pingerPort.getPair())
          .trigger(pong(1), pongerPort.getPair())

          .expect(ping(0), pingerPort, outgoing)
          .expect(Pong.class, pongPredicate(0), pingerPort, incoming)
          .expect(Ping.class, pingPredicate(1), pingerPort, outgoing)
          .expect(pong(1), pingerPort, incoming)

    ;
    //assertEquals(tc.check(), tc.getFinalState());
    assert tc.check_();
  }

  private Ping ping(int count) {
    return new Ping(count);
  }

  private Pong pong(int count) {
    return new Pong(count);
  }

  private Predicate<Pong> pongPredicate(final int count) {
    return new Predicate<Pong>() {
      @Override
      public boolean apply(Pong pong) {
        return pong.count == count;
      }
    };
  }

  private Predicate<Ping> pingPredicate(final int count) {
    return new Predicate<Ping>() {
      @Override
      public boolean apply(Ping ping) {
        return ping.count == count;
      }
    };
  }

  private class PingComparator implements Comparator<Ping> {

    @Override
    public int compare(Ping p1, Ping p2) {
      return p1.count - p2.count;
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null && obj instanceof PingComparator;
    }
  }

  private class PongComparator implements Comparator<Pong> {

    @Override
    public int compare(Pong p1, Pong p2) {
      return p1.count - p2.count;
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null && obj instanceof PongComparator;
    }
  }

  public static class Pinger extends ComponentDefinition {
    { requires(PingPongPort.class); }
  }

  public static class Ponger extends ComponentDefinition {
    { provides(PingPongPort.class); }

  }

  public static class PingPongPort extends PortType {
    {
      request(Ping.class);
      indication(Pong.class);
    }
  }

  public static class Ping implements KompicsEvent{
    int count = 0;
    Ping(int count) {
      this.count = count;
    }

    public String toString() {
      return "Ping " + count;
    }
  }

  public static class Pong implements KompicsEvent{
    int count = 0;
    Pong(int count) {
      this.count = count;
    }

    public String toString() {
      return "Pong " + count;
    }
  }
}
