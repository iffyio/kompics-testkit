package se.sics.kompics.testkit;

import se.sics.kompics.*;
import se.sics.kompics.testkit.fsm.EventQueue;
import se.sics.kompics.testkit.scheduler.CallingThreadScheduler;

public class Proxy extends ComponentDefinition{

  private final EventQueue eventQueue = new EventQueue();

  <T extends ComponentDefinition>
  Proxy() {
    getComponentCore().setScheduler(new CallingThreadScheduler());
  }

  @SuppressWarnings("unchecked")
  <T extends ComponentDefinition> T createComponentUnderTest(
          Class<T> definition, Init<T> initEvent) {
    return (T) create(definition, initEvent).getComponent();
  }

  @SuppressWarnings("unchecked")
  <T extends ComponentDefinition> T createComponentUnderTest(
          Class<T> definition, Init.None initEvent) {
    return (T) create(definition, initEvent).getComponent();
  }

  public EventQueue getEventQueue() {
    return eventQueue;
  }

  <T extends ComponentDefinition> Component createSetupComponent(Class<T> cClass, Init<T> initEvent) {
    Component c = create(cClass, initEvent);
    setupComponent(c);
    return c;
  }

  <T extends ComponentDefinition> Component createSetupComponent(Class<T> cClass, Init.None initEvent) {
    Component c = create(cClass, initEvent);
    setupComponent(c);
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

  private void setupComponent(Component c) {
    // only proxy is scheduled on calling thread
    c.getComponent().getComponentCore().setScheduler(null);
  }

  {
    subscribe(startHandler, control);
  }
}
