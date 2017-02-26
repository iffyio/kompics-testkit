package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import se.sics.kompics.*;
import se.sics.kompics.scheduler.ThreadPoolScheduler;
import se.sics.kompics.testkit.fsm.FSM;

import java.util.Comparator;


public class TestContext<T extends ComponentDefinition> {
  private final Proxy proxy;
  private final ComponentCore proxyComponent;
  private final PortConfig portConfig;
  private T cut;
  private FSM<T> fsm;
  private Scheduler scheduler;
  private boolean checked;


  private TestContext() {
    proxy = new Proxy();
    proxyComponent = proxy.getComponentCore();
    portConfig = new PortConfig(proxy);
  }

  TestContext(Class<T> definition, Init<T> initEvent) {
    this();
    init();
    cut = proxy.createComponentUnderTest(definition, initEvent);
    initFSM();
  }

  TestContext(Class<T> definition, Init.None initEvent) {
    this();
    init();
    cut = proxy.createComponentUnderTest(definition, initEvent);
    initFSM();
  }

  public <T extends ComponentDefinition> Component create(
          Class<T> definition, Init<T> initEvent) {
    Testkit.checkNotNull(definition, initEvent);
    Component c = proxy.createSetupComponent(definition, initEvent);
    fsm.addParticipatingComponents(c);
    return c;
  }

  public <T extends ComponentDefinition> Component create(
          Class<T> definition, Init.None initEvent) {
    Testkit.checkNotNull(definition, initEvent);
    Component c = proxy.createSetupComponent(definition, initEvent);
    fsm.addParticipatingComponents(c);
    return c;
  }

  // // TODO: 2/8/17 connect with channelSelector
  public <P extends PortType> TestContext<T> connect(Negative<P> negative, Positive<P> positive) {
    return connect(positive, negative);
  }

  public <P extends PortType> TestContext<T> connect(Positive<P> positive, Negative<P> negative) {
    return connect(positive, negative, Channel.TWO_WAY);
  }

  <P extends PortType> TestContext<T> connect(
          Negative<P> negative, Positive<P> positive, ChannelFactory factory) {
    return connect(positive, negative, factory);
  }

  <P extends PortType> TestContext<T> connect(
          Positive<P> positive, Negative<P> negative, ChannelFactory factory) {
    Testkit.checkNotNull(positive, negative, factory);

    boolean cutOwnsPositive = positive.getPair().getOwner() == cut.getComponentCore();
    boolean cutOwnsNegative = negative.getPair().getOwner() == cut.getComponentCore();

    // non monitoring ports => connect normally
    if (!(cutOwnsPositive || cutOwnsNegative)) {
      factory.connect((PortCore<P>) positive, (PortCore<P>) negative);
      return this;
    }

    PortCore<P> proxyPort = (PortCore<P>) (cutOwnsPositive? positive : negative);
    PortCore<P> otherPort = (PortCore<P>) (cutOwnsPositive? negative : positive);

    registerConnectedPort(proxyPort == positive, proxyPort, otherPort, factory);
    if (cutOwnsPositive && cutOwnsNegative) {
      registerConnectedPort(otherPort == positive, otherPort, proxyPort, factory);
    }

    return this;
  }


  public TestContext<T> repeat(int times) {
    fsm.repeat(times);
    return this;
  }

  public TestContext<T> end() {
    fsm.endRepeat();
    return this;
  }

  public TestContext<T> body() {
    fsm.body();
    return this;
  }

  public <P extends  PortType> TestContext<T> expect(
          KompicsEvent event, Port<P> port, Direction direction) {
    Testkit.checkNotNull(event, port, direction);
    configurePort(event.getClass(), port, direction);
    fsm.expectMessage(event, port, direction);
    return this;
  }

  public <P extends  PortType, E extends KompicsEvent> TestContext<T> expect(
          Class<E> eventType, Predicate<E> pred, Port<P> port, Direction direction) {
    Testkit.checkNotNull(eventType, port, direction);
    configurePort(eventType, port, direction);
    fsm.expectMessage(eventType, pred, port, direction);
    return this;
  }


  public <P extends PortType> TestContext<T> trigger(
          KompicsEvent event, Port<P> port) {
    Testkit.checkNotNull(event, port);
    if (port.getOwner() == cut.getComponentCore()) {
      throw new IllegalStateException("Triggers are not allowed on component being tested");
    }
    fsm.addTrigger(event, port);
    return this;
  }

  public <P extends  PortType> TestContext<T> disallow(
            KompicsEvent event, Port<P> port, Direction direction) {
    Testkit.checkNotNull(event, port, direction);
    configurePort(event.getClass(), port, direction);
    fsm.addDisallowedEvent(event, port, direction);
    return this;
  }

  public <P extends  PortType> TestContext<T> allow(
            KompicsEvent event, Port<P> port, Direction direction) {
    Testkit.checkNotNull(event, port, direction);
    configurePort(event.getClass(), port, direction);
    fsm.addAllowedEvent(event, port, direction);
    return this;
  }

  public <P extends  PortType> TestContext<T> drop(
            KompicsEvent event, Port<P> port, Direction direction) {
    Testkit.checkNotNull(event, port, direction);
    configurePort(event.getClass(), port, direction);
    fsm.addDroppedEvent(event, port, direction);
    return this;
  }

  public <E extends KompicsEvent> TestContext<T> addComparator(
          Class<E> eventType, Comparator<E> comparator) {
    Testkit.checkNotNull(eventType, comparator);
    fsm.addComparator(eventType, comparator);
    return this;
  }

  public Component getComponentUnderTest() {
    return cut.getComponentCore();
  }

  // capture component type to verify predicate
  public TestContext<T> assertComponentState(
          Predicate<T> assertPred) {
    Testkit.checkNotNull(assertPred);
    fsm.addAssertComponent(assertPred);
    return this;
  }

  public int getFinalState() {
    return fsm.getFinalState();
  }

  public int check() {
    if (checked) {
      throw new IllegalStateException("test has previously been run");
    } else {
      checked = true;
      int errorCode = fsm.start();
      scheduler.shutdown();
      return errorCode;
    }
  }

  // PRIVATE
  private void init() {
    //fsm = new FSM<>(proxy, cut, this);

    // default scheduler
    scheduler = new ThreadPoolScheduler(1);
    Kompics.setScheduler(scheduler);

    // // TODO: 2/20/17 set worker id
    proxyComponent.getControl().doTrigger(Start.event, 0, proxyComponent);
    assert proxyComponent.state() == Component.State.ACTIVE;

  }

  private void initFSM() {
    fsm = new FSM<>(proxy, cut);
    fsm.addParticipatingComponents(cut.getComponentCore());
  }

  private <P extends PortType> void registerConnectedPort(
          boolean isPositive, PortCore<P> port, PortCore<P> other, ChannelFactory factory) {
    PortStructure<P> portStruct = portConfig.getOrCreate(port, isPositive);
    portStruct.addConnectedPort(other, factory);
  }

  private <P extends  PortType> void configurePort(
          Class<? extends KompicsEvent> eventType, Port<P> port, Direction direction) {
    if (port.getOwner() != proxyComponent || port.getPair().getOwner() != cut.getComponentCore()) {
      throw new UnsupportedOperationException("Watching messages are allowed on the tested component's ports " + port);
    }
    PortStructure<P> portStruct = portConfig.get(port);

    if (portStruct == null) {
      if (direction == Direction.INCOMING) {
        throw new IllegalStateException("Can not watch incoming message on an unconnected port " + port);
      } else {
        portStruct = portConfig.create(port);
      }
    }

    if (direction == Direction.OUTGOING) {
      // register outgoing handler
      portStruct.addOutgoingHandler(eventType);
    } else if (direction == Direction.INCOMING){
      // register incoming handler
      portStruct.addIncomingHandler(eventType);
    }
  }
}
