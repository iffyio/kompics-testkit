package se.sics.kompics.testkit;

import org.junit.Test;
import se.sics.kompics.*;

import java.util.Comparator;

public class BasicTest {

  private TestCase tc = TestKit.newTestCase(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private TestKit.Direction incoming = TestKit.Direction.INCOMING;
  private TestKit.Direction outgoing = TestKit.Direction.OUTGOING;

  @Test
  public void work() {
    tc.connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class));
    tc.
      addComparator(Pong.class, new PongComparator()).
      addComparator(Ping.class, new PingComparator()).
      disallow(new Pong(1), pinger.getNegative(PingPongPort.class), incoming).
      allow(new Pong(3), pinger.getNegative(PingPongPort.class), incoming).
      body().
      expect(new Ping(1), pinger.getNegative(PingPongPort.class), outgoing).
      expect(new Pong(2), pinger.getNegative(PingPongPort.class), incoming).
      expect(new Ping(2), pinger.getNegative(PingPongPort.class), outgoing).
      expect(new Pong(3), pinger.getNegative(PingPongPort.class), incoming).

      repeat(1).
        disallow(new Pong(5), pinger.getNegative(PingPongPort.class), incoming).
        disallow(new Pong(15), pinger.getNegative(PingPongPort.class), incoming).
        conditionalDrop(new Pong(60), pinger.getNegative(PingPongPort.class), incoming).
        body().
            repeat(1).
              allow(new Pong(5), pinger.getNegative(PingPongPort.class), incoming).
              allow(new Pong(15), pinger.getNegative(PingPongPort.class), incoming).
              body().
              trigger(new Pong(5), ponger.getPositive(PingPongPort.class).getPair()).
              trigger(new Pong(6), ponger.getPositive(PingPongPort.class).getPair()).
              expect(new Pong(6), pinger.getNegative(PingPongPort.class), incoming).
              trigger(new Pong(15), ponger.getPositive(PingPongPort.class).getPair()).
              expect(new Ping(6), pinger.getNegative(PingPongPort.class), outgoing).
              expect(new Pong(7), pinger.getNegative(PingPongPort.class), incoming).
            end().
        trigger(new Pong(8), ponger.getPositive(PingPongPort.class).getPair()).
        expect(new Pong(8), pinger.getNegative(PingPongPort.class), incoming).
        expect(new Ping(8), pinger.getNegative(PingPongPort.class), outgoing).
        expect(new Pong(9), pinger.getNegative(PingPongPort.class), incoming).
        trigger(new Pong(15), ponger.getPositive(PingPongPort.class).getPair()).
      end().
    check();
  }

  private class PingComparator implements Comparator<Ping> {

    @Override
    public int compare(Ping p1, Ping p2) {
      if (p1.count == p2.count) {
        return 0;
      } else {
        return -1;
      }
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null && obj instanceof PingComparator;
    }
  }

  private class PongComparator implements Comparator<Pong> {

    @Override
    public int compare(Pong p1, Pong p2) {
      if (p1.count == p2.count) {
        return 0;
      } else {
        return -1;
      }
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
        Kompics.logger.error("pinger: Received Pong! {}", pong);
        if (pong.count % 2 == 0)
          trigger(new Ping(pong.count), ppPort);
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) {
        Kompics.logger.info("pinger {}: ", getComponentCore().getComponent().id());
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
        System.out.println("Ponger: received " + ping);
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
