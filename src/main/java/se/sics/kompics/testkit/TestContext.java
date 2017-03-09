package se.sics.kompics.testkit;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import se.sics.kompics.Channel;
import se.sics.kompics.ChannelFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortCore;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.Scheduler;
import se.sics.kompics.Start;
import se.sics.kompics.scheduler.ThreadPoolScheduler;

import se.sics.kompics.testkit.fsm.FSM;

import java.util.Comparator;


public class TestContext<T extends ComponentDefinition> {
  private final Proxy proxy;
  private final ComponentCore proxyComponent;
  private T cut;
  private FSM<T> fsm;
  private Scheduler scheduler;
  private boolean checked;


  private TestContext() {
    proxy = new Proxy();
    proxyComponent = proxy.getComponentCore();
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

  public <P extends PortType> TestContext<T> connect(
          Negative<P> negative, Positive<P> positive) {
    return connect(positive, negative);
  }

  public <P extends PortType> TestContext<T> connect(
          Positive<P> positive, Negative<P> negative) {
    return connect(positive, negative, Channel.TWO_WAY);
  }

  <P extends PortType> TestContext<T> connect(
          Negative<P> negative, Positive<P> positive, ChannelFactory factory) {
    return connect(positive, negative, factory);
  }

  <P extends PortType> TestContext<T> connect(
          Positive<P> positive, Negative<P> negative, ChannelFactory factory) {
    Testkit.checkNotNull(positive, negative, factory);
    fsm.checkInInitialHeader();

    boolean cutOwnsPositive = positive.getPair().getOwner() == cut.getComponentCore();
    boolean cutOwnsNegative = negative.getPair().getOwner() == cut.getComponentCore();

    // non monitored ports => connect normally
    if (!(cutOwnsPositive || cutOwnsNegative)) {
      factory.connect((PortCore<P>) positive, (PortCore<P>) negative);
    } else {
      proxy.doConnect(positive, negative, factory);
    }

    return this;
  }


  public TestContext<T> repeat(int times) {
    fsm.repeat(times);
    return this;
  }

  public TestContext<T> repeat(int times, LoopInit loopInit) {
    Testkit.checkNotNull(loopInit);
    fsm.repeat(times, loopInit);
    return this;
  }

  public TestContext<T> onEachIteration(LoopInit iterationInit) {
    Testkit.checkNotNull(iterationInit);
    fsm.setIterationInit(iterationInit);
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
    checkValidPort(port, direction);
    fsm.expectMessage(event, port, direction);
    return this;
  }

  public <P extends  PortType, E extends KompicsEvent> TestContext<T> expect(
          Class<E> eventType, Predicate<E> pred, Port<P> port, Direction direction) {
    Testkit.checkNotNull(eventType, port, direction);
    checkValidPort(port, direction);
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
    checkValidPort(port, direction);
    fsm.addDisallowedEvent(event, port, direction);
    return this;
  }

  public <P extends  PortType> TestContext<T> allow(
            KompicsEvent event, Port<P> port, Direction direction) {
    Testkit.checkNotNull(event, port, direction);
    checkValidPort(port, direction);
    fsm.addAllowedEvent(event, port, direction);
    return this;
  }

  public <P extends  PortType> TestContext<T> drop(
            KompicsEvent event, Port<P> port, Direction direction) {
    Testkit.checkNotNull(event, port, direction);
    checkValidPort(port, direction);
    fsm.addDroppedEvent(event, port, direction);
    return this;
  }

  public <E extends KompicsEvent> TestContext<T> addComparator(
          Class<E> eventType, Comparator<E> comparator) {
    Testkit.checkNotNull(eventType, comparator);
    fsm.addComparator(eventType, comparator);
    return this;
  }

  public <E extends KompicsEvent> TestContext<T> setDefaultAction(
          Class<E> eventType, Function<E, Action> function) {
    Testkit.checkNotNull(eventType, function);
    fsm.setDefaultAction(eventType, function);
    return this;
  }

  public Component getComponentUnderTest() {
    return cut.getComponentCore();
  }

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


  private <P extends  PortType> void checkValidPort(
          Port<P> port, Direction direction) {
    if (port.getPair().getOwner() != cut.getComponentCore()) {
      throw new UnsupportedOperationException("Watching messages are allowed only on the tested component's ports");
    }

    if (direction == Direction.INCOMING && !proxy.isConnectedPort(port)) {
      throw new IllegalStateException("Cannot watch incoming message on an unconnected port " + port);
    }
  }
}
