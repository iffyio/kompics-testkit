package se.sics.kompics.testkit.urb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.testkit.fd.TAddress;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

import java.io.Serializable;
import java.util.*;

public class UrbComponent extends ComponentDefinition{

  static final Logger logger = LoggerFactory.getLogger(UrbComponent.class);

  public static Set<TAddress> nodes;
  public static Map<TAddress, String> names;

  private final int MAJORITY;
  private final TAddress self;
  private final Set<Data> delivered = new HashSet<>();
  private final Set<Data> pending = new HashSet<>();

  private final Map<Data, Set<TAddress>> ack = new HashMap<>();

  Negative<UrbPort> urbPort = provides(UrbPort.class);
  Positive<Network> network = requires(Network.class);

  public UrbComponent(Init init) {
    self = init.self;
    MAJORITY = nodes.size() / 2 + 1;
  }

  Handler<Start> startHandler = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      //logger.warn("{} started...", self);
      //logger.warn("number of nodes = {}", nodes.size());
    }
  };

  private Handler<URBTimeout> urbTimeoutHandler = new Handler<URBTimeout>() {
    @Override
    public void handle(URBTimeout event) {
      logger.warn("urb timeout");
      tryDeliver();
    }
  };

  private Handler<UrbBroadcast> urbHandler = new Handler<UrbBroadcast>() {
    @Override
    public void handle(UrbBroadcast event) {
      Serializable m = event.message;
      Data data = new Data(self, m);
      pending.add(data);
      ack.put(data, new HashSet<TAddress>());
      bebBroadcast(data);
    }
  };

  private Handler<BebMsg> bebHandler = new Handler<BebMsg>() {
    @Override
    public void handle(BebMsg bebMsg) {
      Data data = bebMsg.data;

      if (!ack.containsKey(data)) {
        ack.put(data, new HashSet<TAddress>());
      }

/*      logger.warn("{}: received msg from {}",
              names.get(self),
              names.get(bebMsg.getSource()));*/

      Set<TAddress> acksForM = ack.get(data);
      acksForM.add(bebMsg.getSource());

      if (!pending.contains(data)) {
        pending.add(data);
        bebBroadcast(data); // acknowledge seen
      }

      tryDeliver();
    }
  };

  private void tryDeliver() {
    for (Data data : pending) {
      if (ack.get(data).size() >= MAJORITY) {
        trigger(new UrbDeliver(data.msg), urbPort);
        delivered.add(data);
      }
    }

    int i = 0;
    for (Data data : delivered) {
      i++;
      pending.remove(data);
    }
/*    logger.warn("delivered {} messages, {} pending",
            delivered.size(), pending.size());*/
  }

  private void bebBroadcast(Data data) {
    for (TAddress dst : nodes) {
      BebMsg bebMsg = new BebMsg(
              self, dst, Transport.TCP, data);
      trigger(bebMsg, network);
      //logger.warn("<<triggered {}", bebMsg);
    }
  }


  {
    subscribe(startHandler, control);
    subscribe(bebHandler, network);
    subscribe(urbHandler, urbPort);
  }


  public static class URBTimeout extends Timeout {
    public URBTimeout(SchedulePeriodicTimeout spt) {
      super(spt);
    }
  }
  public static class Init extends se.sics.kompics.Init<UrbComponent> {
    TAddress self;
    public Init(TAddress self) {
      this.self = self;
    }
  }
}
