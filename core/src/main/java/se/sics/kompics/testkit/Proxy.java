package se.sics.kompics.testkit;

import se.sics.kompics.*;

public class Proxy extends ComponentDefinition{

  <P extends PortType> Negative<P> provideProxy(Class<P> portType) {
    return provides(portType);
  }

  <P extends PortType> Positive<P> requireProxy(Class<P> portType) {
    return requires(portType);
  }

  private Handler<Start> startHandler = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      Kompics.logger.info("Proxy component started");
    }
  };

  {
    subscribe(startHandler, control);
  }
}
