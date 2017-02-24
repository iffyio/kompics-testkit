package se.sics.kompics.testkit;

import se.sics.kompics.*;
import se.sics.kompics.testkit.fsm.EventQueue;
import se.sics.kompics.testkit.scheduler.CallingThreadScheduler;

public class Proxy extends ComponentDefinition{

  private final EventQueue eventQueue = new EventQueue();
  //private ComponentDefinition cut;

  <T extends ComponentDefinition>
  Proxy() {
    getComponentCore().setScheduler(new CallingThreadScheduler());
  }

  <T extends ComponentDefinition> ComponentDefinition createComponentUnderTest(
          Class<T> definition, Init<T> initEvent) {
    return create(definition, initEvent).getComponent();
  }

  <T extends ComponentDefinition> ComponentDefinition createComponentUnderTest(
          Class<T> definition, Init.None initEvent) {
     return create(definition, initEvent).getComponent();
  }

  public EventQueue getEventQueue() {
    return eventQueue;
  }

  <T extends ComponentDefinition> Component createSetupComponent(Class<T> cClass, Init<T> initEvent) {
    Component c = create(cClass, initEvent);
    // only proxy is scheduled on calling thread
    c.getComponent().getComponentCore().setScheduler(null);
    return c;
  }

  <T extends ComponentDefinition> Component createSetupComponent(Class<T> cClass, Init.None initEvent) {
    Component c = create(cClass, initEvent);
    c.getComponent().getComponentCore().setScheduler(null);
    return c;
  }
  <P extends PortType> Negative<P> provideProxy(Class<P> portType) {
    return provides(portType);
  }

  <P extends PortType> Positive<P> requireProxy(Class<P> portType) {
    return requires(portType);
  }

  private Handler<Start> startHandler = new Handler<Start>() {
    @Override
    public void handle(Start event) { }
  };

  {
    subscribe(startHandler, control);
  }
}
