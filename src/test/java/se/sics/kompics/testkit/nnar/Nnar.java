package se.sics.kompics.testkit.nnar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.testkit.fd.TAddress;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Nnar extends ComponentDefinition{

  static final Logger logger = LoggerFactory.getLogger(Nnar.class);
  public static Collection<TAddress> nodes;
  public static Map<TAddress, Integer> rank;
  public static Map<TAddress, String> name;

  Negative<NnarPort> nnarPort = provides(NnarPort.class);
  Positive<Network> network = requires(Network.class);

  private final TAddress self;
  private int MAJORITY;

  private Register register;
  private int acks = 0;
  private Integer writeval = null;
  public int rid = 0;
  private Map<TAddress, Register> readlist = new HashMap<>();
  private Integer readval = null;
  private boolean reading = false;

  public Nnar(Init init) {
    self = init.self;
    register = new Register(0, 0, 0);
    MAJORITY = nodes.size() / 2 + 1;
  }

  private Handler<Read> readHandler = new Handler<Read>() {
    @Override
    public void handle(Read event) {
      assert !reading;
      reading = true;
      broadcastReadRequest();
    }
  };

  private Handler<Write> writeHandler = new Handler<Write>() {
    @Override
    public void handle(Write write) {
      writeval = write.val;
      broadcastReadRequest();
    }
  };

  private Handler<WriteRequest> writeRequestHandler = new Handler<WriteRequest>() {
    @Override
    public void handle(WriteRequest writeRequest) {
      if (writeRequest.register.greaterThan(register)) {
        register = writeRequest.register;
      }

      trigger(new Ack(self, writeRequest.getSource(), Transport.TCP,
              writeRequest.rid), network);
    }
  };

  private Handler<Ack> ackHandler = new Handler<Ack>() {
    @Override
    public void handle(Ack ack) {
      if (ack.rid != rid) {
        return;
      }

      acks++;
      if (acks >= MAJORITY) {
        acks = 0;
        if (reading) {
          reading = false;
          trigger(new ReadReturn(readval), nnarPort);
        } else {
          trigger(new WriteReturn(), nnarPort);
        }
      }
    }
  };

  private void broadcastReadRequest() {
    rid++;
    acks = 0;
    readlist.clear();
    for (TAddress node : nodes) {
      trigger(new ReadRequest(self, node, Transport.TCP, rid), network);
    }
  }

  private Handler<ReadRequest> readRequestHandler = new Handler<ReadRequest>() {
    @Override
    public void handle(ReadRequest event) {
      logger.warn("{}: sending readValue", name.get(self));
      trigger(new ReadValue(self, event.getSource(),
              Transport.TCP, rid, register), network);
    }
  };

  private Handler<ReadValue> readValueHandler = new Handler<ReadValue>() {
    @Override
    public void handle(ReadValue readValue) {
      if (readValue.rid != rid) {
        return;
      }

      readlist.put(readValue.getSource(), readValue.register);
      if (readlist.size() >= MAJORITY) {
        Register maxRegister = highest();
        readlist.clear();

        int maxts = maxRegister.ts;
        int maxrank = maxRegister.writeRank;
        int maxVal = maxRegister.val;

        if (!reading) {
          maxts++;
          maxrank = rank.get(self);
          maxVal = writeval;
        }

        Register r = new Register(maxts, maxrank, maxVal);

        for (TAddress node : nodes) {
          trigger(new WriteRequest(self, node, Transport.TCP, rid, r), network);
        }
      }
    }
  };

  private Register highest() {
    Register highestSoFar = null;
    for (Register r : readlist.values()) {
      if (highestSoFar == null || r.greaterThan(highestSoFar)) {
        highestSoFar = r;
      }
    }
    return highestSoFar;
  }

  Handler<Start> startHandler = new Handler<Start>() {
    @Override
    public void handle(Start event) { }
  };

  {
    subscribe(startHandler, control);
    subscribe(readHandler, nnarPort);
    subscribe(writeHandler, nnarPort);

    subscribe(readRequestHandler, network);
    subscribe(readValueHandler, network);
    subscribe(writeRequestHandler, network);
    subscribe(ackHandler, network);
  }

  public static class Init extends se.sics.kompics.Init<Nnar> {
    TAddress self;
    public Init(TAddress self) {
      this.self = self;
    }
  }
}
