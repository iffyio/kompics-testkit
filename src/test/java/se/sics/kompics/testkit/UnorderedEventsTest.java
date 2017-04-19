package se.sics.kompics.testkit;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.junit.Before;
import org.junit.Test;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.testkit.pingpong.Ping;
import se.sics.kompics.testkit.pingpong.PingComparator;
import se.sics.kompics.testkit.pingpong.PingPongPort;
import se.sics.kompics.testkit.pingpong.Pinger;
import se.sics.kompics.testkit.pingpong.Pong;
import se.sics.kompics.testkit.pingpong.PongComparator;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;

import static org.junit.Assert.assertEquals;
import static se.sics.kompics.testkit.Direction.*;

import java.util.Random;

public class UnorderedEventsTest {

  private TestContext<Pinger> tc;
  private Negative<PingPongPort> pingerPort;

  private Component pinger, ponger1, ponger2, ponger3;

  private Ping ping = new Ping(0);
  private Pong pong1 = new Pong(1);
  private Pong pong2 = new Pong(2);
  private Pong pong3 = new Pong(3);

  @Before
  public void init() {
    tc = Testkit.newTestContext(Pinger.class, Init.NONE);
    pinger = tc.getComponentUnderTest();
    ponger1 = tc.create(Ponger.class, new PongerInit(1));
    ponger2 = tc.create(Ponger.class, new PongerInit(2));
    ponger3 = tc.create(Ponger.class, new PongerInit(3));

    pingerPort = pinger.getNegative(PingPongPort.class);
    tc.connect(pingerPort, ponger1.getPositive(PingPongPort.class));
    tc.connect(pingerPort, ponger2.getPositive(PingPongPort.class));
    tc.connect(pingerPort, ponger3.getPositive(PingPongPort.class));

    tc.addComparator(Ping.class, new PingComparator()).
       addComparator(Pong.class, new PongComparator());
  }

  private void connectTimers() {
    Component timer1 = tc.create(JavaTimer.class, Init.NONE);
    Component timer3 = tc.create(JavaTimer.class, Init.NONE);
    Component timer2 = tc.create(JavaTimer.class, Init.NONE);
    tc.connect(ponger1.getNegative(Timer.class), timer1.getPositive(Timer.class));
    tc.connect(ponger2.getNegative(Timer.class), timer2.getPositive(Timer.class));
    tc.connect(ponger3.getNegative(Timer.class), timer3.getPositive(Timer.class));
  }

  @Test
  public void unorderedTest() {
    connectTimers();

    tc.setDefaultAction(Ping.class, new Function<Ping, Action>() {
         @Override
         public Action apply(Ping ping) {
           return ping.count == 0? Action.HANDLE : Action.DROP;
         }
    }).body();

    tc.repeat(10).body().
       trigger(ping, pingerPort.getPair()).
       expect(ping, pingerPort, OUTGOING).

            unordered().
            expect(pong1, pingerPort, INCOMING).
            expect(pong2, pingerPort, INCOMING).
            expect(pong3, pingerPort, INCOMING).
       end().
    end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  private ScheduleTimeout st = new ScheduleTimeout(500);
  private RandomTimeout timeout = new RandomTimeout(st);

  @Test
  public void expectWithinSingleBlockTest() {
    initExpectWithinBlock();

    tc.blockExpect(Pong.class, predicateForPong(1), pingerPort, INCOMING).
            blockExpect(pong2, pingerPort, INCOMING).
       body().
         trigger(ping, pingerPort.getPair()).
         expect(ping, pingerPort, OUTGOING).
         trigger(timeout, ponger1.getNegative(Timer.class)).
         trigger(timeout, ponger3.getNegative(Timer.class)).
         trigger(timeout, ponger2.getNegative(Timer.class)).
         expect(pong3, pingerPort, INCOMING);

    assertEquals(tc.check(), tc.getFinalState());
  }

  private Predicate<Pong> predicateForPong(final int count) {
    return new Predicate<Pong>() {
      @Override
      public boolean apply(Pong pong) {
        return pong.count == count;
      }

      @Override
      public String toString() {
        return "Predicate(Pong(" + count + "))";
      }
    };
  }

  private void initExpectWithinBlock() {
    tc.setDefaultAction(Ping.class, new Function<Ping, Action>() {
      @Override
      public Action apply(Ping ping) {
        return Action.DROP;
      }
    }); // drop pings that weren't triggered
  }

  @Test
  public void expectWithinEmptyBlockTest() {
    connectTimers();
    initExpectWithinBlock();
    tc.body().
        repeat(10).
        body().
          trigger(ping, pingerPort.getPair()).
          expect(ping, pingerPort, OUTGOING).
          repeat(1).
            blockExpect(Pong.class, predicateForPong(1), pingerPort, INCOMING).
            blockExpect(Pong.class, predicateForPong(2), pingerPort, INCOMING).
            blockExpect(pong3, pingerPort, INCOMING).
          body().
          end().
        end();
    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void ExpectWithinMultipleBlockTest() {
    tc.setTimeout(Ponger.timeout);
    connectTimers();
    initExpectWithinBlock();
    tc.body().
       repeat(10).
            blockExpect(pong1, pingerPort, INCOMING).
       body().
          trigger(ping, pingerPort.getPair()).
          repeat(1).
            blockExpect(pong2, pingerPort, INCOMING).
          body().
             expect(ping, pingerPort, OUTGOING).
             repeat(1).
            blockExpect(pong3, pingerPort, INCOMING).
             body().
             end().
          end().
       end();
    assertEquals(tc.check(), tc.getFinalState());
  }

  public static class Ponger extends ComponentDefinition {

    private static long timeout = 10;
    private Random random = new Random(System.nanoTime());
    private Positive<Timer> timer = requires(Timer.class);
    private Negative<PingPongPort> ppPort = provides(PingPongPort.class);
    private int id;

    public Ponger(PongerInit init) {
      id = init.id;
    }

    private Handler<Ping> pingHandler = new Handler<Ping>() {
      @Override
      public void handle(Ping ping) {
        setTimer();
      }
    };

    private Handler<RandomTimeout> timeoutHandler = new Handler<RandomTimeout>() {
      @Override
      public void handle(RandomTimeout timeout) {
        trigger(new Pong(id), ppPort);
      }
    };

    private void setTimer() {
      long delay = random.nextInt((int) timeout);
      ScheduleTimeout st = new ScheduleTimeout(delay);
      RandomTimeout timeout = new RandomTimeout(st);
      st.setTimeoutEvent(timeout);
      trigger(st, timer);
    }

    {
      subscribe(timeoutHandler, timer);
      subscribe(pingHandler, ppPort);
    }
  }

  public static class PongerInit extends Init<Ponger> {
    int id;
    public PongerInit(int id) {
      this.id = id;
    }
  }

  private static class RandomTimeout extends Timeout {
    RandomTimeout(ScheduleTimeout st) {
      super(st);
    }
  }
}
