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
    Negative pingerNegative = pinger.getNegative(PingPongPort.class);
    Positive pongerPositive = ponger.getPositive(PingPongPort.class);

    tc.connect(pingerNegative, pongerPositive);
    tc.
      addComparator(Pong.class, new PongComparator()).
      addComparator(Ping.class, new PingComparator()).
      disallow(new Pong(1), pingerNegative, incoming).
      allow(new Pong(3), pingerNegative, incoming).

      body().
        expect(new Ping(1), pingerNegative, outgoing).
        expect(Pong.class, pongPredicate(2), pingerNegative, incoming).
        expect(Ping.class, pingPredicate(2), pingerNegative, outgoing).
        expect(new Pong(3), pingerNegative, incoming).

        repeat(1).

          disallow(new Pong(5), pingerNegative, incoming).
          disallow(new Pong(15), pingerNegative, incoming).
          drop(new Pong(60), pingerNegative, incoming).

          body().
              repeat(1).
                allow(new Pong(5), pingerNegative, incoming).
                allow(new Pong(15), pingerNegative, incoming).
                body().

                trigger(new Pong(5), pongerPositive.getPair()).
                trigger(new Pong(6), pongerPositive.getPair()).
                expect(Pong.class, pongPredicate(6), pingerNegative, incoming).
                trigger(new Pong(15), pongerPositive.getPair()).
                expect(Ping.class, pingPredicate(6), pingerNegative, outgoing).
                expect(new Pong(7), pingerNegative, incoming).
              end().
          trigger(new Pong(8), pongerPositive.getPair()).
          expect(new Pong(8), pingerNegative, incoming).
          expect(new Ping(8), pingerNegative, outgoing).
          expect(Pong.class, pongPredicate(9), pingerNegative, incoming).
          trigger(new Pong(15), pongerPositive.getPair()).
        end();

    assertEquals(tc.check(), tc.getFinalState());
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
    Positive<PingPongPort> ppPort = requires(PingPongPort.class);
    static int k = 0;
    int i = 0;
    Handler<Pong> pongHandler = new Handler<Pong>() {
      @Override
      public void handle(Pong pong) {
        //Kompics.logger.error("pinger: Received Pong! {}", pong);
        if (pong.count % 2 == 0)
          trigger(new Ping(pong.count), ppPort);
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) {
        //Kompics.logger.info("pinger {}: ", getComponentCore().getComponent().id());
        trigger(new Ping(1), ppPort);
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
        //System.out.println("Ponger: received " + ping);
        trigger(new Pong(ping.count + 1), pingPongPort);
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
