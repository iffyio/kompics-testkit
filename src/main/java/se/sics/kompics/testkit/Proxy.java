package se.sics.kompics.testkit;

import se.sics.kompics.*;
import se.sics.kompics.testkit.fsm.EventQueue;
import se.sics.kompics.testkit.scheduler.CallingThreadScheduler;

import java.util.Map;

public class Proxy extends ComponentDefinition{

  private final EventQueue eventQueue = new EventQueue();
  private PortConfig portConfig;
  private Component cut;

  <T extends ComponentDefinition>
  Proxy() {
    getComponentCore().setScheduler(new CallingThreadScheduler());
  }

  @SuppressWarnings("unchecked")
  <T extends ComponentDefinition> T createComponentUnderTest(
          Class<T> definition, Init<T> initEvent) {
    cut = create(definition, initEvent);
    createPortConfig();
    return (T) cut.getComponent();
  }

  @SuppressWarnings("unchecked")
  <T extends ComponentDefinition> T createComponentUnderTest(
          Class<T> definition, Init.None initEvent) {
    cut = create(definition, initEvent);
    createPortConfig();
    return (T) cut.getComponent();
  }

  Component getComponentUnderTest() {
    return cut;
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

  <P extends PortType> Negative<P> providePort(Class<P> portType) {
    return provides(portType);
  }

  <P extends PortType> Positive<P> requirePort(Class<P> portType) {
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

  private void createPortConfig() {
    portConfig = new PortConfig(this);
  }

  public Map<Class<? extends PortType>, JavaPort<? extends PortType>> getCutPositivePorts() {
    return ((JavaComponent) cut).positivePorts;
  }

  public Map<Class<? extends PortType>, JavaPort<? extends PortType>> getCutNegativePorts() {
    return ((JavaComponent) cut).negativePorts;
  }

  public <P extends PortType> void connectPorts(Positive<P> positive,
                                                Negative<P> negative,
                                                ChannelFactory factory) {
    portConfig.connectPorts(positive, negative, factory);
  }

}
