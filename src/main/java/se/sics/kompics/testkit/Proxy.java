package se.sics.kompics.testkit;

import se.sics.kompics.*;
import se.sics.kompics.testkit.fsm.EventQueue;
import se.sics.kompics.testkit.scheduler.CallingThreadScheduler;

public class Proxy extends ComponentDefinition{

  private final EventQueue eventQueue = new EventQueue();
  private ComponentDefinition cut;
  Class<? extends ComponentDefinition> cutClass;

  <T extends ComponentDefinition>
  Proxy(Class<T> cutClass, Init<T> initEvent) {
    // // TODO: 2/8/17 nosuchmethodexception with initEvent when Init.NONE
    //cut = create(cutClass, initEvent).getComponent();
    this.cutClass = cutClass;
    getComponentCore().setScheduler(new CallingThreadScheduler());
  }

  void createComponentUnderTest() {
    cut = create(cutClass, Init.NONE).getComponent();
  }

  ComponentDefinition getCut() {
    return cut;
  }

  public EventQueue getEventQueue() {
    return eventQueue;
  }

  <T extends ComponentDefinition> Component createNewSetupComponent(Class<T> cClass, Init<T> initEvent) {
    Component x = create(cClass, initEvent);
    x.getComponent().getComponentCore().setScheduler(null);
    Kompics.logger.warn("created: {}, state is {}", x, x.state());
    return x;
  }

  <T extends ComponentDefinition> Component createNewSetupComponent(Class<T> cClass, Init.None initEvent) {
    Component x = create(cClass, initEvent);
    x.getComponent().getComponentCore().setScheduler(null);
    return x;
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
