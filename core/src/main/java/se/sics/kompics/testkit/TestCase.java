package se.sics.kompics.testkit;

import se.sics.kompics.*;
import se.sics.kompics.scheduler.ThreadPoolScheduler;
import se.sics.kompics.testkit.fsm.ExpectState;
import se.sics.kompics.testkit.fsm.FSM;
import se.sics.kompics.testkit.fsm.Trigger;


class TestCase {
  private final Proxy proxy;
  private final ComponentCore proxyComponent;
  private final ComponentDefinition cut;
  private final PortConfig portConfig;
  private FSM fsm;
  private ThreadPoolScheduler scheduler;
  private boolean checked;

  <T extends ComponentDefinition> TestCase(
          Class<T> cutClass, Init<T> initEvent) {
    proxy = new Proxy(cutClass, initEvent);
    proxyComponent = proxy.getComponentCore();
    cut = proxy.getCut();
    portConfig = new PortConfig(proxy);
    fsm = new FSM(proxy);
    scheduler = new ThreadPoolScheduler(1);
    Kompics.setScheduler(scheduler);
  }

  Component getComponentUnderTest() {
    return cut.getComponentCore();
  }

  <T extends ComponentDefinition> Component create(
          Class<T> cutClass, Init<T> initEvent) {
    return proxy.createNewSetupComponent(cutClass, initEvent);
  }

  // // TODO: 2/8/17 create with init, config
  <T extends ComponentDefinition> Component create(
          Class<T> cutClass, Init.None initEvent) {
    return proxy.createNewSetupComponent(cutClass, initEvent);
  }

  // // TODO: 2/8/17 connect with channel, channelSelector
  <P extends PortType> TestCase connect( Negative<P> negative, Positive<P> positive) {
    return connect(positive, negative);
  }

  <P extends PortType> TestCase connect(Positive<P> positive, Negative<P> negative) {
    boolean cutOwnsPositive = positive.getPair().getOwner() == cut.getComponentCore();
    boolean cutOwnsNegative = negative.getPair().getOwner() == cut.getComponentCore();

    // non monitoring ports => connect normally
    if (!(cutOwnsPositive || cutOwnsNegative)) {
      Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative);
      return this;
    }

    PortCore<P> proxyPort = (PortCore<P>) (cutOwnsPositive? positive : negative);
    PortCore<P> otherPort = (PortCore<P>) (cutOwnsPositive? negative : positive);

    addConnectedPort(proxyPort == positive, proxyPort, otherPort);
    if (cutOwnsPositive && cutOwnsNegative) {
      addConnectedPort(otherPort == positive, otherPort, proxyPort);
    }

    return this;
  }

  private <P extends PortType> void addConnectedPort(
          boolean isPositive, PortCore<P> port, PortCore<P> other) {
    PortStructure<P> portStruct = portConfig.getOrCreate(port, isPositive);
    portStruct.addConnectedPort(other, Channel.TWO_WAY);
  }



  <P extends  PortType> TestCase expect(
          KompicsEvent event, Port<P> port, TestKit.Direction direction) {
    if (port.getOwner() != proxyComponent) {
      // // TODO: 2/8/17 support inside ports as well
      throw new UnsupportedOperationException("Expect messages are supported only for the component being tested");
    }
    PortStructure<P> portStruct = portConfig.get(port);

    if (portStruct == null) {
      if (direction == TestKit.Direction.INCOMING) {
        throw new IllegalStateException("Can not expect incoming message on an unconnected port");
      } else {
        portStruct = portConfig.create(port);
      }
    }

    if (direction == TestKit.Direction.OUTGOING) {
      // register outgoing handler
      portStruct.addOutgoingHandler(event);
    } else if (direction == TestKit.Direction.INCOMING){
      // register incoming handler
      portStruct.addIncomingHandler(event);
    }

    fsm.addStateToFSM(new ExpectState(fsm, new EventSpec(event, port, direction)));

    return this;
  }

  <P extends PortType> TestCase trigger(
          KompicsEvent event, Port<P> port) {
    // register state
    fsm.addStateToFSM(new Trigger(event, port));
    return this;
  }

  TestCase repeat(int times) {
    fsm.repeat(times);
    return this;
  }

  TestCase endRepeat() {
    fsm.endRepeat();
    return this;
  }

  TestCase disallow(EventSpec eventSpec) {
    fsm.blacklist(eventSpec);
    return this;
  }

  TestCase allow(EventSpec eventSpec) {
    fsm.whitelist(eventSpec);
    return this;
  }

  TestCase conditionalDrop(EventSpec eventSpec) {
    fsm.conditionalDrop(eventSpec);
    return this;
  }


  void check() {
    if (checked) {
      throw new IllegalStateException("test has previously been run");
    } else {
      checked = true;
      fsm.start();
      scheduler.shutdown();
    }
  }
}
