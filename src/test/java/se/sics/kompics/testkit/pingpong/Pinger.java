package se.sics.kompics.testkit.pingpong;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;

public class Pinger extends ComponentDefinition {
  private Positive<PingPongPort> ppPort = requires(PingPongPort.class);

  private Handler<Pong> pongHandler = new Handler<Pong>() {
    @Override
    public void handle(Pong pong) {
      trigger(new Ping(pong.count + 1), ppPort);
    }
  };

  {
    subscribe(pongHandler, ppPort);
  }
}
