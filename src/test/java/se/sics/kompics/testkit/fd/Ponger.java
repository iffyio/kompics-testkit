package se.sics.kompics.testkit.fd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

public class Ponger extends ComponentDefinition {
  private static final Logger logger = LoggerFactory.getLogger(Ponger.class);

  Positive<Network> network = requires(Network.class);
  TAddress self;

  public Ponger() {
    init();
  }

  private void init() {
    self = Util.getPongerAddr();
  }

  int i = 0;
  private Handler<Ping> pingHandler = new Handler<Ping>() {
    @Override
    public void handle(Ping ping) {
      logger.warn("Ponger: received {}", ping);
      //if (i++ % 2 == 0)
      trigger(new Pong(self, ping.getSource()), network);
    }
  };

  private Handler<Start> startHandler = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      logger.warn("ponger started {}", self);
    }
  };

  {
    subscribe(startHandler, control);
    subscribe(pingHandler, network);
  }
}
