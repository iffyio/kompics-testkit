package se.sics.kompics.testkit;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;
import se.sics.kompics.*;

import java.util.Comparator;

public class RequestResponseTest {

  private static boolean replyAfterNPongs = false;
  private static int numberOfRequiredPongs = 2;

  private TestCase tc;
  private Component pinger;
  private Component pinger2;
  private Component ponger;
  private Component ponger2;

  private Direction incoming = Direction.INCOMING;
  private Direction outgoing = Direction.OUTGOING;

  @Test
  public void componentSendsRequest() {
    replyAfterNPongs = true;
    tc = TestKit.newTestCase(Pinger.class, Init.NONE);
    pinger = tc.getComponentUnderTest();
    pinger2 = tc.create(Pinger.class, Init.NONE);
    ponger = tc.create(Ponger.class, Init.NONE);
    ponger2 = tc.create(Ponger.class, Init.NONE);
    int count = 0;

    tc.connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      connect(pinger.getNegative(PingPongPort.class), ponger2.getPositive(PingPongPort.class)).
      connect(pinger2.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      connect(pinger2.getNegative(PingPongPort.class), ponger2.getPositive(PingPongPort.class));

    tc.
      addComparator(Ping.class, new PingComparator()).
      body().
      repeat(100).body().
        expect(new Ping(count), pinger.getNegative(PingPongPort.class), outgoing).
        expect(new Pong(new Ping(count)), pinger.getNegative(PingPongPort.class), incoming).
        expect(new Pong(new Ping(count)), pinger.getNegative(PingPongPort.class), incoming).
      end();

    assertEquals(tc.getFinalState(), tc.check());
  }

  @Test
  public void componentSendsResponse() {
    replyAfterNPongs = false;

    tc = TestKit.newTestCase(Ponger.class, Init.NONE);
    ponger = tc.getComponentUnderTest();
    ponger2 = tc.create(Ponger.class, Init.NONE);
    pinger = tc.create(Pinger.class, Init.NONE);
    pinger2 = tc.create(Pinger.class, Init.NONE);

    tc.connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class));
    tc.connect(pinger2.getNegative(PingPongPort.class), ponger2.getPositive(PingPongPort.class));

    tc.addComparator(Ping.class, new PingComparator()).body();

    //// TODO: 2/21/17 write testcase for 1-to-1 channel forwarding on outbound - response
    tc.
      repeat(5).
      body().
        expect(new Ping(0), ponger.getPositive(PingPongPort.class), incoming).
        expect(new Pong(new Ping(0)), ponger.getPositive(PingPongPort.class), outgoing).
      end();

    assertEquals(tc.getFinalState(), tc.check());
  }

  public static class Pinger extends ComponentDefinition {
    public Pinger() {}
    private int count = 0;
    private int replies = 0;
    Positive<PingPongPort> ppPort = requires(PingPongPort.class);
    Handler<Pong> pongHandler = new Handler<Pong>() {
      @Override
      public void handle(Pong event) {
        Kompics.logger.error("pinger: Received Pong!{}", event);
        if (RequestResponseTest.replyAfterNPongs) {
          replies++;
          if (replies == RequestResponseTest.numberOfRequiredPongs) {
            replies = 0;
            trigger(new Ping(0), ppPort);
          }
        } else {
          trigger(new Ping(0), ppPort);
        }
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) {
        trigger(new Ping(count++), ppPort);
        Kompics.logger.warn("Pinger {} started", this);
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
        //Kompics.logger.warn("Ponger {}: received {}", this, ping);
        Pong pong = new Pong(RequestResponseTest.cloneRequest(ping));
        trigger(pong, pingPongPort);
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) { }
    };

    {
      subscribe(pingHandler, pingPongPort);
      subscribe(startHandler, control);
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
    Ping(int count) {
      this.count = count;
    }

    public String toString() {
      return "Ping(" + count + ")";
    }
  }

  static class Pong extends Response {
    private Ping ping;
    Pong(Request ping) {
      super(ping);
      this.ping = (Ping) ping;
    }
    public boolean equals(Object o) {
      return o instanceof Pong && ping.count == ((Pong) o).ping.count;
    }

    public int hashCode() {
      return this.getClass().hashCode();
    }

    public String toString() {
      return "Pong(" + ping.count + ")";
    }
  }

  private class PingComparator implements Comparator<Ping> {
    @Override
    public int compare(Ping p1, Ping p2) {
      return p1.count - p2.count;
    }
  }

  public static Request cloneRequest(Request request) {
    Request requestClone = null;
    try {
      requestClone = (Request) request.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    return requestClone;
  }
}
