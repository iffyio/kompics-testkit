package se.sics.kompics.testkit.pingpong;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;

public class Ponger extends ComponentDefinition {
  private Negative<PingPongPort> pingPongPort = provides(PingPongPort.class);

  private Handler<Ping> pingHandler = new Handler<Ping>() {
    @Override
    public void handle(Ping ping) {
      trigger(new Pong(ping.count), pingPongPort);
    }
  };

  {
    subscribe(pingHandler, pingPongPort);
  }
}
