package se.sics.kompics.testkit;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import se.sics.kompics.Channel;
import se.sics.kompics.ChannelFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
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

import java.util.Comparator;


public class TestContext<T extends ComponentDefinition> {
  private final Proxy<T> proxy;
  private final ComponentCore proxyComponent;
  private T cut;
  private FSM<T> fsm;
  private Scheduler scheduler;
  private boolean checked;


  private TestContext(Init<? extends ComponentDefinition> initEvent, Class<T> definition) {
    proxy = new Proxy<T>();
    proxyComponent = proxy.getComponentCore();
    init();
    if (initEvent == Init.NONE) {
      cut = proxy.createComponentUnderTest(definition, (Init.None) initEvent);
    } else {
      cut = proxy.createComponentUnderTest(definition, (Init<T>) initEvent);
    }
    initFSM();
  }

  TestContext(Class<T> definition, Init<T> initEvent) {
    this(initEvent, definition);
  }

  TestContext(Class<T> definition, Init.None initEvent) {
    this(initEvent, definition);
  }

  public <T extends ComponentDefinition> Component create(
          Class<T> definition, Init<T> initEvent) {
    Testkit.checkNotNull(definition, initEvent);
    Component c = proxy.createSetupComponent(definition, initEvent);
    fsm.addParticipant(c);
    return c;
  }

  public <T extends ComponentDefinition> Component create(
          Class<T> definition, Init.None initEvent) {
    Testkit.checkNotNull(definition, initEvent);
    Component c = proxy.createSetupComponent(definition, initEvent);
    fsm.addParticipant(c);
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

  public TestContext<T> repeat(int times, BlockInit blockInit) {
    Testkit.checkNotNull(blockInit);
    fsm.repeat(times, blockInit);
    return this;
  }

  public TestContext<T> onEachIteration(BlockInit iterationInit) {
    Testkit.checkNotNull(iterationInit);
    fsm.setIterationInit(iterationInit);
    return this;
  }

  public TestContext<T> end() {
    fsm.end();
    return this;
  }

  public TestContext<T> body() {
    fsm.body();
    return this;
  }

  public TestContext<T> unordered() {
    fsm.setUnorderedMode();
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
    Testkit.checkNotNull(eventType, port, pred, direction);
    checkValidPort(port, direction);
    fsm.expectMessage(eventType, pred, port, direction);
    return this;
  }

  public <P extends  PortType> TestContext<T> blockExpect(
          KompicsEvent event, Port<P> port, Direction direction) {
    Testkit.checkNotNull(event, port, direction);
    checkValidPort(port, direction);
    fsm.expectWithinBlock(event, port, direction);
    return this;
  }

  public <P extends  PortType, E extends KompicsEvent> TestContext<T> blockExpect(
          Class<E> eventType, Predicate<E> pred, Port<P> port, Direction direction) {
    Testkit.checkNotNull(eventType, pred, port, direction);
    checkValidPort(port, direction);
    fsm.expectWithinBlock(eventType, pred, port, direction);
    return this;
  }

  public TestContext<T> expectWithMapper() {
    fsm.setExpectWithMapperMode();
    return this;
  }

  public <E extends KompicsEvent, R extends KompicsEvent> TestContext<T> setMapperForNext(
          int expectedEvents, Class<E> eventType, Function<E, R> mapper) {
    Testkit.checkNotNull(eventType, mapper);
    fsm.setMapperForNext(expectedEvents, eventType, mapper);
    return this;
  }

  public TestContext<T> expect(
          Port<? extends PortType> listenPort, Port<? extends PortType> responsePort) {
    Testkit.checkNotNull(listenPort, responsePort);
    checkValidPort(listenPort, Direction.OUTGOING);
    fsm.addExpectWithMapper(listenPort, responsePort);
    return this;
  }

  public <E extends KompicsEvent, R extends KompicsEvent> TestContext<T> expect(
          Class<E> eventType, Port<? extends PortType> listenPort,
          Port<? extends PortType> responsePort, Function<E, R> mapper) {
    Testkit.checkNotNull(eventType, listenPort, responsePort, mapper);
    checkValidPort(listenPort, Direction.OUTGOING);
    fsm.addExpectWithMapper(eventType, listenPort, responsePort, mapper);
    return this;
  }

  public TestContext<T> expectWithFuture() {
    fsm.setExpectWithFutureMode();
    return this;
  }

  public <E extends KompicsEvent, R extends KompicsEvent> TestContext<T> expect(
          Class<E> eventType, Port<? extends PortType> listenPort, Future<E, R> future) {
    Testkit.checkNotNull(eventType, listenPort, future);
    checkValidPort(listenPort, Direction.OUTGOING);
    fsm.addExpectWithFuture(eventType, listenPort, future);
    return this;
  }

  public <E extends KompicsEvent, R extends KompicsEvent, P extends PortType> TestContext<T> trigger(
          Port<P> responsePort, Future<E, R> future) {
    Testkit.checkNotNull(responsePort, future);
    fsm.addTrigger(responsePort, future);
    return this;
  }

  public TestContext<T> either() {
    fsm.either();
    return this;
  }

  public TestContext<T> or() {
    fsm.or();
    return this;
  }

  public <P extends PortType> TestContext<T> trigger(
          KompicsEvent event, Port<P> port) {
    Testkit.checkNotNull(event, port);
    fsm.addTrigger(event, port);
    return this;
  }

  // // TODO: 3/31/17 allow matching predicate
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

  public TestContext<T> inspect(
          Predicate<T> assertPred) {
    Testkit.checkNotNull(assertPred);
    fsm.inspect(assertPred);
    return this;
  }

  public TestContext<T> expectFault(
          Class<? extends Throwable> exceptionType, Fault.ResolveAction resolveAction) {
    Testkit.checkNotNull(exceptionType, resolveAction);
    fsm.expectFault(exceptionType, resolveAction);
    return this;
  }

  public TestContext<T> expectFault(
          Predicate<Throwable> exceptionPredicate, Fault.ResolveAction resolveAction) {
    Testkit.checkNotNull(exceptionPredicate, resolveAction);
    fsm.expectFault(exceptionPredicate, resolveAction);
    return this;
  }

  public int getFinalState() {
    return fsm.getFinalState();
  }

  public TestContext<T> setTimeout(long timeoutMS) {
    proxy.eventQueue.setTimeout(timeoutMS);
    return this;
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
    fsm = proxy.getFsm();
    fsm.addParticipant(cut.getComponentCore());
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
