package se.sics.kompics.testkit;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import se.sics.kompics.Component;
import se.sics.kompics.Init;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.testkit.fd.*;
import se.sics.kompics.testkit.fsm.FSM;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

import java.util.Comparator;

public class EPFDTest {
  private TestKit.Direction outgoing = TestKit.Direction.OUTGOING;
  private TestKit.Direction incoming = TestKit.Direction.INCOMING;
  private ScheduleTimeout st = new ScheduleTimeout(500);
  private EPFD.PingTimeout timeout = new EPFD.PingTimeout(st);

  private TAddress pongerAddr = Util.getPongerAddr();
  private TAddress epfdAddr = Util.getEPFDAddr();
  private Pong pong = new Pong(pongerAddr, epfdAddr);
  private Ping ping = new Ping(epfdAddr, pongerAddr);
  private Suspect suspect = new Suspect(pongerAddr);
  private Restore restore = new Restore(pongerAddr);

  private TestCase tc = TestKit.newTestCase(EPFD.class, Init.NONE);
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private Component epfd = tc.getComponentUnderTest();

  @Test
  public void mockNetworkAndTimer() {

    tc.addComparator(ScheduleTimeout.class, new ScheduleTimeoutComparator()).
       body();
    // Expect initial SCHED TIMEOUT
    tc.expect(st, epfd.getNegative(Timer.class), outgoing);

    // SEND Watch P
    Watch watch = new Watch(pongerAddr);
    tc.trigger(watch, epfd.getPositive(EPFDPort.class)); // WATCH P

    // Send Timeout, Reply with then PING, then SCHEDTIMEOUT
    tc.trigger(timeout, epfd.getNegative(Timer.class)).
    expect(ping, epfd.getNegative(Network.class), outgoing).
    expect(st, epfd.getNegative(Timer.class), outgoing);

    tc.repeat(30).body().
        // Send Timeout, Reply with Suspect, then PING, then SCHEDTIMEOUT
        trigger(timeout, epfd.getNegative(Timer.class)).
        expect(suspect, epfd.getPositive(EPFDPort.class), outgoing).
        expect(ping, epfd.getNegative(Network.class), outgoing).
        expect(st, epfd.getNegative(Timer.class), outgoing).

        // Send Pong, Send Timeout, Reply with RESTORE, then PING, then SCHEDTIMEOUT
        trigger(pong, epfd.getNegative(Network.class)).
        trigger(timeout, epfd.getNegative(Timer.class)).
        expect(restore, epfd.getPositive(EPFDPort.class), outgoing).
        expect(ping, epfd.getNegative(Network.class), outgoing).
        expect(st, epfd.getNegative(Timer.class), outgoing).
    end();

    //Send Pong, Timeout, Reply with PING, then SCHEDTIMEOUT
    tc.repeat(20).body().
        disallow(restore, epfd.getPositive(EPFDPort.class), outgoing).
        disallow(suspect, epfd.getPositive(EPFDPort.class), outgoing).
            //body().
        trigger(pong, epfd.getNegative(Network.class)).
        trigger(timeout, epfd.getNegative(Timer.class)).
        expect(ping, epfd.getNegative(Network.class), outgoing).
        expect(st, epfd.getNegative(Timer.class), outgoing).
    end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  private void mockTimerOnly() {
    // Connect EPFD, Ponger
    Component networkPonger = tc.create(NettyNetwork.class, new NettyInit(pongerAddr));
    Component networkEPFD = tc.create(NettyNetwork.class, new NettyInit(epfdAddr));
    tc.connect(epfd.getNegative(Network.class), networkEPFD.getPositive(Network.class));
    tc.connect(ponger.getNegative(Network.class), networkPonger.getPositive(Network.class));

    // Expect initial SCHED TIMEOUT
    tc.expect(st, epfd.getNegative(Timer.class), outgoing);
    tc.disallow(restore, epfd.getPositive(EPFDPort.class), outgoing);

    // SEND Watch P
    Watch watch = new Watch(pongerAddr);
    tc.trigger(watch, epfd.getPositive(EPFDPort.class)); // WATCH P

    // Send Timeout, Reply with then PING, then SCHEDTIMEOUT
    triggerTimeoutExpectPingAndSchedTimeout();

    // Expect Pong, Send Timeout, Reply with PING, then SCHEDTIMEOUT
    tc.expect(pong, epfd.getNegative(Network.class), incoming);
    triggerTimeoutExpectPingAndSchedTimeout();

    // drop Pongs within scope, then timeout and suspect
    tc.repeat(100);
            tc.conditionalDrop(pong, epfd.getNegative(Network.class), incoming).
            trigger(timeout, epfd.getNegative(Timer.class)).
            expect(suspect, epfd.getPositive(EPFDPort.class), outgoing). // resuspect
            expect(ping, epfd.getNegative(Network.class), outgoing).
            expect(st, epfd.getNegative(Timer.class), outgoing);
    tc.end();

    // expectUntil restore?
/*    tc.expect(pong, epfd.getNegative(Network.class), incoming);
    tc.expect(restore, epfd.getPositive(EPFDPort.class), outgoing);*/


    assertEquals(tc.check(), tc.getFinalState());
  }

  private void triggerTimeoutExpectPingAndSchedTimeout() {
    tc.trigger(timeout, epfd.getNegative(Timer.class)).
    expect(ping, epfd.getNegative(Network.class), outgoing).
    expect(st, epfd.getNegative(Timer.class), outgoing);
  }

  public EPFDTest() {
    st.setTimeoutEvent(timeout);
  }

  private class ScheduleTimeoutComparator implements Comparator<ScheduleTimeout> {

    @Override
    public int compare(ScheduleTimeout o1, ScheduleTimeout o2) {
      return 0;
    }
  }
  public static void main(String... a) {
    new EPFDTest().runTest();
  }

  private void runTest() {
    mockNetworkAndTimer();
  }
}
