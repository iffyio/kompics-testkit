package se.sics.kompics.testkit;

import com.google.common.base.Function;
import org.junit.Before;
import org.junit.Test;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
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

  @Before
  public void init() {
    tc = Testkit.newTestContext(Pinger.class, Init.NONE);
    Component pinger = tc.getComponentUnderTest();
    Component ponger1, ponger2, ponger3;
    ponger1 = tc.create(Ponger.class, new PongerInit(1));
    ponger2 = tc.create(Ponger.class, new PongerInit(2));
    ponger3 = tc.create(Ponger.class, new PongerInit(3));

    pingerPort = pinger.getNegative(PingPongPort.class);
    tc.connect(pingerPort, ponger1.getPositive(PingPongPort.class));
    tc.connect(pingerPort, ponger2.getPositive(PingPongPort.class));
    tc.connect(pingerPort, ponger3.getPositive(PingPongPort.class));

    Component timer1 = tc.create(JavaTimer.class, Init.NONE);
    Component timer3 = tc.create(JavaTimer.class, Init.NONE);
    Component timer2 = tc.create(JavaTimer.class, Init.NONE);
    tc.connect(ponger1.getNegative(Timer.class), timer1.getPositive(Timer.class));
    tc.connect(ponger2.getNegative(Timer.class), timer2.getPositive(Timer.class));
    tc.connect(ponger3.getNegative(Timer.class), timer3.getPositive(Timer.class));
  }

  @Test
  public void unorderedTest() {
    tc.addComparator(Ping.class, new PingComparator()).
       addComparator(Pong.class, new PongComparator()).
       setDefaultAction(Ping.class, new Function<Ping, Action>() {
         @Override
         public Action apply(Ping ping) {
           return ping.count == 0? Action.HANDLE : Action.DROP;
         }
       }).
    body();

    Ping ping = new Ping(0);
    Pong pong1 = new Pong(1);
    Pong pong2 = new Pong(2);
    Pong pong3 = new Pong(3);

    tc.repeat(10).body().
       trigger(ping, pingerPort.getPair()).
       expect(ping, pingerPort, OUTGOING).

       ignoreOrder().
            expect(pong1, pingerPort, INCOMING).
            expect(pong2, pingerPort, INCOMING).
            expect(pong3, pingerPort, INCOMING).
       end().
    end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  public static class Ponger extends ComponentDefinition {

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
      long delay = random.nextInt(400);
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
