package se.sics.kompics.testkit;

import org.junit.Test;
import se.sics.kompics.Component;
import se.sics.kompics.Init;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.testkit.fd.TAddress;
import se.sics.kompics.testkit.fd.Util;
import se.sics.kompics.testkit.urb.*;
import se.sics.kompics.timer.SchedulePeriodicTimeout;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class UrbTest {

  TAddress[] addresses = new TAddress[4];
  TestContext<UrbComponent> tc = null;
  private Direction incoming = Direction.INCOMING;
  private Direction outgoing = Direction.OUTGOING;

  private Component p, q, r, s;
  private TAddress pAddr, qAddr, rAddr, sAddr;
  private String[] names = {"p", "q", "r", "s"};

  {
    setAddresses();
  }

  private void setAddresses() {

    Set<TAddress> nodes = new HashSet<>();
    Map<TAddress, String> m = new HashMap<>();
    for (int i = 0; i < addresses.length; i++) {
      TAddress addr = Util.createAddress(2345 + i);
      addresses[i] = addr;
      nodes.add(addr);
      m.put(addr, names[i]);
    }

    UrbComponent.names = m;
    UrbComponent.nodes = nodes;

    pAddr = addresses[0];
    qAddr = addresses[1];
    rAddr = addresses[2];
    sAddr = addresses[3];
  }

  @Test
  public void run() throws IOException {

    tc = Testkit.newTestContext(UrbComponent.class, new UrbComponent.Init(pAddr));
    p = tc.getComponentUnderTest();
    setup(pAddr, p);

    q = createURBComponent(qAddr);
    r = createURBComponent(rAddr);
    s = createURBComponent(sAddr);

    tc.
      addComparator(BebMsg.class, new BebComparator()).
      addComparator(UrbDeliver.class, new URBDeliverComparator()).
      allow(bebMsg(pAddr, pAddr, 0), p.getNegative(Network.class), outgoing).
      allow(bebMsg(pAddr, qAddr, 0), p.getNegative(Network.class), outgoing).
      allow(bebMsg(pAddr, rAddr, 0), p.getNegative(Network.class), outgoing).
      allow(bebMsg(pAddr, sAddr, 0), p.getNegative(Network.class), outgoing).

      allow(bebMsg(pAddr, pAddr, 0), p.getNegative(Network.class), incoming).
      allow(bebMsg(qAddr, pAddr, 0), p.getNegative(Network.class), incoming).
      allow(bebMsg(rAddr, pAddr, 0), p.getNegative(Network.class), incoming).
      allow(bebMsg(sAddr, pAddr, 0), p.getNegative(Network.class), incoming).
      body();
      //expect(spt, p.getNegative(Timer.class), outgoing);

    tc.
      repeat(10).body().
      trigger(new UrbBroadcast(0), p.getPositive(UrbPort.class)).

/*      expect(bebMsg(pAddr, pAddr, 0), p.getNegative(Network.class), outgoing).
      expect(bebMsg(pAddr, qAddr, 0), p.getNegative(Network.class), outgoing).
      expect(bebMsg(pAddr, rAddr, 0), p.getNegative(Network.class), outgoing).
      expect(bebMsg(pAddr, sAddr, 0), p.getNegative(Network.class), outgoing).

      expect(bebMsg(pAddr, pAddr, 0), p.getNegative(Network.class), incoming).
      expect(bebMsg(rAddr, pAddr, 0), p.getNegative(Network.class), incoming).
      expect(bebMsg(qAddr, pAddr, 0), p.getNegative(Network.class), incoming).
      expect(bebMsg(sAddr, pAddr, 0), p.getNegative(Network.class), incoming).*/

      expect(new UrbDeliver(0), p.getPositive(UrbPort.class), outgoing).
      end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  private BebMsg bebMsg(TAddress src, TAddress dst, int c) {
    return new BebMsg(src, dst, Transport.TCP, new Data(src, c));
  }



  private Component createURBComponent(TAddress self) {
    Component c = tc.create(UrbComponent.class, new UrbComponent.Init(self));
    setup(self, c);
    return c;
  }

  private void setup(TAddress self, Component urb) {
    Component network = tc.create(NettyNetwork.class, new NettyInit(self));
    tc.connect(urb.getNegative(Network.class), network.getPositive(Network.class));
  }

  private class SchedulePeriodicTimeoutComparator implements Comparator<SchedulePeriodicTimeout> {
    @Override
    public int compare(SchedulePeriodicTimeout o1, SchedulePeriodicTimeout o2) {
      return 0;
    }
  }

  private class BebComparator implements Comparator<BebMsg> {

    @Override
    public int compare(BebMsg m1, BebMsg m2) {
      if (m1.getSource().equals(m2.getSource()) &&
          m1.getDestination().equals(m2.getDestination()) &&
          m1.data.equals(m2.data)) {
        return 0;
      }

      return -1;
    }
  }

  private class URBDeliverComparator implements Comparator<UrbDeliver> {

    @Override
    public int compare(UrbDeliver o1, UrbDeliver o2) {
      if (o1.message.equals(o2.message)) {
        return 0;
      }
      return -1;
    }
  }
}
