package se.sics.kompics.testkit;

import org.junit.Test;
import se.sics.kompics.Component;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.testkit.pingpong.Ping;
import se.sics.kompics.testkit.pingpong.PingComparator;
import se.sics.kompics.testkit.pingpong.PingPongPort;
import se.sics.kompics.testkit.pingpong.Pinger;
import se.sics.kompics.testkit.pingpong.Pong;
import se.sics.kompics.testkit.pingpong.PongComparator;
import se.sics.kompics.testkit.pingpong.Ponger;

import static junit.framework.Assert.assertEquals;

public class BasicTest {

  private TestContext<Pinger> tc = Testkit.newTestContext(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private Direction incoming = Direction.INCOMING;
  private Direction outgoing = Direction.OUTGOING;
  private Negative<PingPongPort> pingerPort = pinger.getNegative(PingPongPort.class);
  private Positive<PingPongPort> pongerPort = ponger.getPositive(PingPongPort.class);

  @Test
  public void dropAllowDisallowTest() {
    tc.connect(pingerPort, pongerPort);
    tc.
      addComparator(Pong.class, new PongComparator()).
      addComparator(Ping.class, new PingComparator()).
      disallow(pong(0), pingerPort, outgoing).
      allow(pong(0), pingerPort, incoming).
      body().
            trigger(ping(0), pongerPort).
            expect(ping(1), pingerPort, outgoing).
            expect(pong(1), pingerPort, incoming).
      repeat(1).
        disallow(pong(3), pingerPort, incoming).
        drop(ping(2), pingerPort, outgoing).
        drop(ping(4), pingerPort, outgoing). // last msg on inner repeat
        body().
            trigger(ping(1), pingerPort.getPair()).
            repeat(1).
              allow(ping(4), pingerPort, outgoing).
              allow(pong(4), pingerPort, incoming).
              drop(ping(5), pingerPort, outgoing).
              body().
              trigger(ping(4), pingerPort.getPair()).
              expect(ping(1), pingerPort, outgoing).
              expect(pong(1), pingerPort, incoming).

/*              expect(ping(2), pingerPort, outgoing). // expect dropped msg
              expect(pong(2), pingerPort, incoming).*/

              expect(ping(3), pingerPort, outgoing).
              expect(pong(3), pingerPort, incoming).
            end().
        trigger(ping(1), pingerPort.getPair()).
        expect(ping(1), pingerPort, outgoing).
        expect(pong(1), pingerPort, incoming).
        expect(ping(2), pingerPort, outgoing).
      end();

    //assertEquals(tc.check(), tc.getFinalState());
    assert tc.check_();
  }

  private Ping ping(int i) {
    return new Ping(i);
  }

  private Pong pong(int i) {
    return new Pong(i);
  }

}
