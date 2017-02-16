package se.sics.kompics.testkit;

import org.junit.Test;
import se.sics.kompics.Component;
import se.sics.kompics.Init;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.testkit.fd.*;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.testkit.TestCase;
import se.sics.kompics.testkit.TestKit;

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

    // Expect initial SCHED TIMEOUT
    tc.expect(st, epfd.getNegative(Timer.class), outgoing);

    // SEND Watch P
    Watch watch = new Watch(pongerAddr);
    tc.trigger(watch, epfd.getPositive(EPFDPort.class)); // WATCH P

    // Send Timeout, Reply with then PING, then SCHEDTIMEOUT
    tc.trigger(timeout, epfd.getNegative(Timer.class)).
    expect(ping, epfd.getNegative(Network.class), outgoing).
    expect(st, epfd.getNegative(Timer.class), outgoing);

    tc.repeat(30).
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
    endRepeat();

    //Send Pong, Timeout, Reply with PING, then SCHEDTIMEOUT
    tc.repeat(20).
        disallow(restore, epfd.getPositive(EPFDPort.class), outgoing).
        disallow(suspect, epfd.getPositive(EPFDPort.class), outgoing).
            //body().
        trigger(pong, epfd.getNegative(Network.class)).
        trigger(timeout, epfd.getNegative(Timer.class)).
        expect(ping, epfd.getNegative(Network.class), outgoing).
        expect(st, epfd.getNegative(Timer.class), outgoing).
    endRepeat();

    tc.check();
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
    tc.endRepeat();

    // expectUntil restore?
/*    tc.expect(pong, epfd.getNegative(Network.class), incoming);
    tc.expect(restore, epfd.getPositive(EPFDPort.class), outgoing);*/


    tc.check();
  }

  private void triggerTimeoutExpectPingAndSchedTimeout() {
    tc.trigger(timeout, epfd.getNegative(Timer.class)).
    expect(ping, epfd.getNegative(Network.class), outgoing).
    expect(st, epfd.getNegative(Timer.class), outgoing);
  }

  public EPFDTest() {
    st.setTimeoutEvent(timeout);
  }

  public static void main(String... a) {
    new EPFDTest().runTest();
  }

  private void runTest() {
    mockNetworkAndTimer();
  }
}
