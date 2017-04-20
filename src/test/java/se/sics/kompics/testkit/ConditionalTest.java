package se.sics.kompics.testkit;

import org.junit.Test;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;

import se.sics.kompics.testkit.pingpong.Ping;
import se.sics.kompics.testkit.pingpong.PingComparator;
import se.sics.kompics.testkit.pingpong.Pong;
import se.sics.kompics.testkit.pingpong.PongComparator;

import static junit.framework.Assert.assertEquals;
import static se.sics.kompics.testkit.Direction.*;

public class ConditionalTest {
  private TestContext<Pinger> tc = Testkit.newTestContext(Pinger.class, Init.NONE);
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Negative<PingPongPort> pingerPort = pinger.getNegative(PingPongPort.class);
  private Positive<PingPongPort> pongerPort = ponger.getPositive(PingPongPort.class);
  {
    tc.connect(pingerPort, pongerPort);
    tc.addComparator(Ping.class, new PingComparator());
    tc.addComparator(Pong.class, new PongComparator());
  }

  @Test
  public void basicEitherTest() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(pong(4), pongerPort.getPair())
        .trigger(pong(5), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    basicExpect();
  }

  @Test
  public void basicOrTest() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(ping(2), pingerPort.getPair())
        .trigger(ping(3), pingerPort.getPair())
        .trigger(ping(4), pingerPort.getPair())
        .trigger(ping(5), pingerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    basicExpect();
  }

  private void basicExpect() {
    tc.expect(pong(0), pingerPort, INCOMING);

    tc.either()
        .expect(pong(1), pingerPort, INCOMING)
        .expect(pong(2), pingerPort, INCOMING)
        .expect(pong(4), pingerPort, INCOMING)
        .expect(pong(5), pingerPort, INCOMING)
    .or()
        .expect(pong(1), pingerPort, INCOMING)
        .expect(ping(2), pingerPort, OUTGOING)
        .expect(ping(3), pingerPort, OUTGOING)
        .expect(ping(4), pingerPort, OUTGOING)
        .expect(ping(5), pingerPort, OUTGOING)
        .expect(pong(1), pingerPort, INCOMING)
    .end();

    tc.expect(ping(6), pingerPort, OUTGOING);

    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void nestedEither1() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(pong(3), pongerPort.getPair())
        .trigger(pong(9), pongerPort.getPair())
        .trigger(ping(10), pingerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    nestedExpect();
  }

  @Test
  public void nestedEither2() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(pong(3), pongerPort.getPair())
        .trigger(ping(11), pingerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    nestedExpect();
  }

  @Test
  public void nestedEither3() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(pong(3), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    nestedExpect();
  }

  @Test
  public void nestedEither4() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(pong(3), pongerPort.getPair())
        .trigger(ping(11), pingerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    nestedExpect();
  }

  @Test
  public void nestedOrTest1() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(ping(3), pingerPort.getPair())
        .trigger(pong(5), pongerPort.getPair())
        .trigger(pong(6), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    nestedExpect();
  }

  @Test
  public void nestedOrTest2() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(ping(3), pingerPort.getPair())
        .trigger(ping(4), pingerPort.getPair())
        .trigger(pong(6), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    nestedExpect();
  }

  @Test
  public void nestedOrTest3() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(ping(5), pingerPort.getPair())
        .trigger(pong(6), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    nestedExpect();
  }

  @Test
  public void nestedOrTest4() {
    tc
        .body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair())
        .trigger(pong(6), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    nestedExpect();
  }

  private void nestedExpect() {
    tc.expect(pong(0), pingerPort, INCOMING);

    tc.either()
        .expect(pong(1), pingerPort, INCOMING)
        .expect(pong(2), pingerPort, INCOMING)
        .either()
            .expect(pong(3), pingerPort, INCOMING)
            .either()
              .expect(pong(9), pingerPort, INCOMING)
              .expect(ping(10), pingerPort, OUTGOING)
            .or()
              .expect(ping(11), pingerPort, OUTGOING)
            .end()
        .or()
            .expect(pong(3), pingerPort, INCOMING)
        .end()
    .or()
        .expect(pong(1), pingerPort, INCOMING)
        .expect(pong(2), pingerPort, INCOMING)
        .either()
            .either()
                .expect(ping(3), pingerPort, OUTGOING)
                .expect(pong(5), pingerPort, INCOMING)
            .or()
                .expect(ping(3), pingerPort, OUTGOING)
                .expect(ping(4), pingerPort, OUTGOING)
            .end()
        .or()
            .either()
                .expect(ping(5), pingerPort, OUTGOING)
            .or()
                .expect(ping(6), pingerPort, OUTGOING)
            .end()
        .end()
        .expect(pong(6), pingerPort, INCOMING)
    .end();

    tc.expect(ping(6), pingerPort, OUTGOING);

    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void basicNestedLoopTest() {

    tc.body();
    tc.trigger(pong(3), pongerPort.getPair())

        .either()
            .expect(pong(3), pingerPort, INCOMING)
            .either()
                .expect(pong(9), pingerPort, INCOMING)
                .expect(ping(10), pingerPort, OUTGOING)
            .or()
                .expect(ping(11), pingerPort, OUTGOING)
            .end()
        .or()
            .expect(pong(3), pingerPort, INCOMING)
        .end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  public static Ping ping(int i) {
    return new Ping(i);
  }

  public static Pong pong(int i) {
    return new Pong(i);
  }

  public static class Pinger extends ComponentDefinition {
    {requires(PingPongPort.class);}
  }

  public static class Ponger extends ComponentDefinition {
    {provides(PingPongPort.class);}
  }

  public static class PingPongPort extends PortType {
    {
      request(Ping.class);
      indication(Pong.class);
    }
  }
}
