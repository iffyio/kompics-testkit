package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import org.junit.Test;
import se.sics.kompics.Component;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.testkit.fd.TAddress;
import se.sics.kompics.testkit.fd.Util;
import se.sics.kompics.testkit.nnar.Ack;
import se.sics.kompics.testkit.nnar.Nnar;
import se.sics.kompics.testkit.nnar.NnarPort;
import se.sics.kompics.testkit.nnar.Read;
import se.sics.kompics.testkit.nnar.ReadRequest;
import se.sics.kompics.testkit.nnar.ReadReturn;
import se.sics.kompics.testkit.nnar.ReadValue;
import se.sics.kompics.testkit.nnar.Register;
import se.sics.kompics.testkit.nnar.WriteRequest;


import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

public class NnarTest {

  TAddress[] addresses = new TAddress[1];
  TestContext<Nnar> tc = null;
  private Direction incoming = Direction.INCOMING;
  private Direction outgoing = Direction.OUTGOING;

  private Component p;
  private TAddress pAddr;
  private String[] names = {"p", "q", "r", "s"};

  {
    init();
  }

  private void init() {
    setAddresses();

    tc = Testkit.newTestContext(Nnar.class, new Nnar.Init(pAddr));
    p = tc.getComponentUnderTest();
    setup(pAddr, p);
  }

  private void setAddresses() {

    Set<TAddress> nodes = new HashSet<>();
    Map<TAddress, String> name = new HashMap<>();
    Map<TAddress, Integer> rank = new HashMap<>();

    for (int i = 0; i < addresses.length; i++) {
      TAddress addr = Util.createAddress(2345 + i);
      addresses[i] = addr;
      nodes.add(addr);
      rank.put(addr, i);
      name.put(addr, this.names[i]);
    }

    Nnar.name = name;
    Nnar.rank = rank;
    Nnar.nodes = nodes;

    pAddr = addresses[0];
  }

  private ReadRequest rr1 = readRequest(pAddr, pAddr, 1);
  private Register reg0 = new Register(0, 0, 0);

  @Test
  public void terminationRead() {
    tc.addComparator(ReadRequest.class, new ReadRequestComparator()).
       addComparator(ReadValue.class, new ReadValueComparator()).
       addComparator(WriteRequest.class, new WriteRequestComparator()).
       addComparator(Ack.class, new AckComparator()).
       addComparator(ReadReturn.class, new ReadReturnComparator()).

      body().
      trigger(new Read(), p.getPositive(NnarPort.class)).

      assertComponentState(ridWasIncremented).

      expect(rr1, p.getNegative(Network.class), outgoing).
      expect(rr1, p.getNegative(Network.class), incoming).

      expect(readValue(pAddr, pAddr, 1, reg0), p.getNegative(Network.class), outgoing).
      expect(readValue(pAddr, pAddr, 1, reg0), p.getNegative(Network.class), incoming).

      expect(writeRequest(pAddr, pAddr, 1, reg0), p.getNegative(Network.class), outgoing).
      expect(writeRequest(pAddr, pAddr, 1, reg0), p.getNegative(Network.class), incoming).

      expect(ack(pAddr, pAddr, 1), p.getNegative(Network.class), outgoing).
      expect(Ack.class, ack1Predicate, p.getNegative(Network.class), incoming).

      expect(new ReadReturn(0), p.getPositive(NnarPort.class), outgoing);

    assertEquals(tc.check(), tc.getFinalState());
  }

  private Predicate<Nnar> ridWasIncremented = new Predicate<Nnar>() {
    int rid = 0;
    @Override
    public boolean apply(Nnar nnar) {
      rid++;
      return nnar.rid == rid;
    }
  };

  private Predicate<Ack> ack1Predicate = new Predicate<Ack>() {
    @Override
    public boolean apply(Ack ack) {
      return ack.rid == 1;
    }
  };

  private class ReadReturnComparator implements Comparator<ReadReturn> {

    @Override
    public int compare(ReadReturn r1, ReadReturn r2) {
      if (r1.val == r2.val) {
        return 0;
      }
      return -1;
    }
  }
  private WriteRequest writeRequest(TAddress src, TAddress dst, int rid, Register register) {
    return new WriteRequest(src, dst, Transport.TCP, rid, register);
  }

  private ReadValue readValue(TAddress src, TAddress dst, int rid, Register register) {
    return new ReadValue(src, dst, Transport.TCP, rid, register);
  }

  private Ack ack(TAddress src, TAddress dst, int rid) {
    return new Ack(src, dst, Transport.TCP, rid);
  }

  private class AckComparator implements Comparator<Ack> {
    @Override
    public int compare(Ack a1, Ack a2) {
      if (a1.getSource().equals(a2.getSource()) &&
              a1.getDestination().equals(a2.getDestination()) &&
              a1.rid == a2.rid) {
        return 0;
      }
      return -1;
    }
  }
  private class WriteRequestComparator implements Comparator<WriteRequest> {
    @Override
    public int compare(WriteRequest r1, WriteRequest r2) {
      if (r1.getSource().equals(r2.getSource()) &&
          r1.getDestination().equals(r2.getDestination()) &&
          r1.rid == r2.rid && r1.register.equals(r2.register)) {
        return 0;
      }
      return -1;
    }
  }

  private class ReadValueComparator implements Comparator<ReadValue> {

    @Override
    public int compare(ReadValue r1, ReadValue r2) {
      if (r1.getSource().equals(r2.getSource()) &&
          r1.getDestination().equals(r2.getDestination()) &&
          r1.rid == r2.rid && r1.register.equals(r2.register)) {
        return 0;
      }
      return -1;
    }
  }
  private class ReadRequestComparator implements Comparator<ReadRequest> {

    @Override
    public int compare(ReadRequest r1, ReadRequest r2) {
      if ( r1.getSource().equals(r2.getSource()) &&
           r1.getDestination().equals(r2.getDestination()) &&
           r1.rid == r2.rid) {
        return 0;
      }
      return -1;
    }
  }

  private ReadRequest readRequest(TAddress src, TAddress dst, int rid) {
    return new ReadRequest(src, dst, Transport.TCP, rid);
  }

  private Component createNNarComponent(TAddress self) {
    Component c = tc.create(Nnar.class, new Nnar.Init(self));
    setup(self, c);
    return c;
  }

  private void setup(TAddress self, Component urb) {
    Component network = tc.create(NettyNetwork.class, new NettyInit(self));
    tc.connect(urb.getNegative(Network.class), network.getPositive(Network.class));
  }
}
