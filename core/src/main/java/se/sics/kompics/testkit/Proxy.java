package se.sics.kompics.testkit;

import se.sics.kompics.*;
import se.sics.kompics.testkit.fsm.EventQueue;

public class Proxy extends ComponentDefinition{

  private final EventQueue eventQueue = new EventQueue();
  private final ComponentDefinition cut;

  <T extends ComponentDefinition>
  Proxy(Class<T> cutClass, Init<T> initEvent) {
    // // TODO: 2/8/17 nosuchmethodexception with initEvent when Init.NONE
    //cut = create(cutClass, initEvent).getComponent();
    cut = create(cutClass, Init.NONE).getComponent();
  }

  ComponentDefinition getCut() {
    return cut;
  }

  public EventQueue getEventQueue() {
    return eventQueue;
  }

  <T extends ComponentDefinition> Component createNewSetupComponent(Class<T> cClass, Init<T> initEvent) {
    return create(cClass, initEvent);
  }

  <T extends ComponentDefinition> Component createNewSetupComponent(Class<T> cClass, Init.None initEvent) {
    return create(cClass, initEvent);
  }
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
