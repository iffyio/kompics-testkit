package se.sics.kompics.testkit;

import com.google.common.base.Function;
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
    tc.body().repeat(10).body()
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
    tc.body().repeat(10).body()
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

    tc.expect(ping(6), pingerPort, OUTGOING).end(); // end repeat

    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void nestedEither1() {
    tc.body().repeat(10).body()
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
    tc.body().repeat(10).body()
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
    tc.body().repeat(10).body()
        .trigger(pong(0), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(pong(3), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair());

    nestedExpect();
  }

  @Test
  public void nestedEither4() {
    tc.body().repeat(10).body()
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
    tc.body().repeat(10).body()
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
    tc.body().repeat(10).body()
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
    tc.body().repeat(10).body()
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
    tc.body().repeat(10).body()
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

    tc.expect(ping(6), pingerPort, OUTGOING).end(); // end repeat

    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void basicNestedLoopTest() {

    tc.body();
    tc.trigger(pong(3), pongerPort.getPair());

    tc.either()
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

  @Test
  public void conditionalUnorderedEitherTest() {
    tc.body().repeat(10).body()
        .trigger(pong(2), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(ping(6), pingerPort.getPair())
        .trigger(pong(4), pongerPort.getPair())
        .trigger(ping(5), pingerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(pong(2), pongerPort.getPair());

    conditionalUnorderedTest();
  }

  @Test
  public void conditionalUnorderedOrTest() {
    tc.body().repeat(10).body()
        .trigger(pong(2), pongerPort.getPair())
        .trigger(pong(1), pongerPort.getPair())
        .trigger(ping(1), pingerPort.getPair())
        .trigger(pong(2), pongerPort.getPair())
        .trigger(ping(3), pingerPort.getPair())
        .trigger(pong(2), pongerPort.getPair());

    conditionalUnorderedTest();
  }

  private void conditionalUnorderedTest() {
    tc.expect(pong(2), pingerPort, INCOMING);
    tc.either()
        .expect(pong(1), pingerPort, INCOMING)
        .unordered()
            .expect(pong(4), pingerPort, INCOMING)
            .expect(ping(5), pingerPort, OUTGOING)
            .expect(ping(6), pingerPort, OUTGOING)
        .end()
        .expect(pong(1), pingerPort, INCOMING)
    .or()
        .expect(pong(1), pingerPort, INCOMING)
        .unordered()
            .expect(ping(3), pingerPort, OUTGOING)
            .expect(ping(1), pingerPort, OUTGOING)
            .expect(pong(2), pingerPort, INCOMING)
        .end()
    .end();
    tc.expect(pong(2), pingerPort, INCOMING).end(); // end repeat

    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void conditionalExpectMapperTest() {
    tc.body();
    tc
        .trigger(ping(1), pingerPort.getPair())
        .trigger(ping(4), pingerPort.getPair())
        .trigger(ping(2), pingerPort.getPair())
        .trigger(ping(3), pingerPort.getPair())
        .trigger(ping(3), pingerPort.getPair())
        .trigger(ping(5), pingerPort.getPair())
        .trigger(pong(9), pongerPort.getPair());

    conditionalExpectWithResponse();
  }

  @Test
  public void conditionalExpectFutureTest() {
    tc.body();
    tc
        .trigger(ping(1), pingerPort.getPair())
        .trigger(ping(6), pingerPort.getPair())
        .trigger(ping(2), pingerPort.getPair())
        .trigger(ping(3), pingerPort.getPair())
        .trigger(ping(3), pingerPort.getPair())
        .trigger(ping(7), pingerPort.getPair())
        .trigger(pong(9), pongerPort.getPair());

    conditionalExpectWithResponse();
  }

  private void conditionalExpectWithResponse() {

    tc.expect(ping(1), pingerPort, OUTGOING);
    tc.either()
        .expect(ping(4), pingerPort, OUTGOING)

        .expectWithMapper()
            .setMapperForNext(2, Ping.class, pingPongMapper)
            .expect(pingerPort, pingerPort)
            .expect(Ping.class, pingerPort, pingerPort, pingPongMapper)
            .expect(pingerPort, pingerPort)
        .end()

        .expect(ping(5), pingerPort, OUTGOING)
    .or()
        .expect(ping(6), pingerPort, OUTGOING)

        .expectWithFuture()
            .expect(Ping.class, pingerPort, future1)
            .expect(Ping.class, pingerPort, future2)
            .expect(Ping.class, pingerPort, future3)
        .end()

        .expect(ping(7), pingerPort, OUTGOING)
    .end();
    tc.expect(pong(9), pingerPort, INCOMING);

    assertEquals(tc.check(), tc.getFinalState());
  }

  private PFuture future1 = new PFuture();
  private PFuture future2 = new PFuture();
  private PFuture future3 = new PFuture();

  private class PFuture extends Future<Ping, Pong> {
    Pong pong;

    @Override
    public void set(Ping request) {
      pong = pingPongMapper.apply(request);
    }

    @Override
    public Pong get() {
      return pong;
    }
  };

  private Function<Ping, Pong> pingPongMapper = new Function<Ping, Pong>() {
    @Override
    public Pong apply(Ping ping) {
      return new Pong(ping.count);
    }
  };


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
