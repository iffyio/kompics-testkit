package se.sics.kompics.testkit;

import se.sics.kompics.*;
import se.sics.kompics.scheduler.ThreadPoolScheduler;
import se.sics.kompics.testkit.fsm.FSM;
import se.sics.kompics.testkit.fsm.Trigger;


class TestCase {
  private final Parent parent;
  private final Component parentComponent;
  private final ComponentDefinition cut;
  private final PortConfig portConfig;
  private FSM fsm;
  private ThreadPoolScheduler scheduler;

  <T extends ComponentDefinition> TestCase(
          Class<T> cutClass, Init<T> initEvent) {
    parent = new Parent(cutClass, initEvent);
    parentComponent = parent.getComponentCore();
    cut = parent.getCut();
    portConfig = new PortConfig(parent);
    fsm = new FSM((ComponentCore) parentComponent);
    scheduler = new ThreadPoolScheduler(1);
    Kompics.setScheduler(scheduler);
  }

  Component getComponentUnderTest() {
    return cut.getComponentCore();
  }

  <T extends ComponentDefinition> Component create(
          Class<T> cutClass, Init<T> initEvent) {
    return parent.createNewSetupComponent(cutClass, initEvent);
  }

  // // TODO: 2/8/17 create with init, config
  <T extends ComponentDefinition> Component create(
          Class<T> cutClass, Init.None initEvent) {
    return parent.createNewSetupComponent(cutClass, initEvent);
  }

  // // TODO: 2/8/17 connect with channel, channelSelector
  <P extends PortType> TestCase connect( Negative<P> negative, Positive<P> positive) {
    return connect(positive, negative);
  }

  <P extends PortType> TestCase connect(Positive<P> positive, Negative<P> negative) {
    boolean posIsParent = positive.getOwner() == parentComponent;
    boolean negIsParent = negative.getOwner() == parentComponent;
    // non interfacing ports
    if (!(posIsParent || negIsParent)) {
      Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative);
      return this;
    }

    PortCore<P> parentPort = (PortCore<P>) (posIsParent? positive : negative);
    PortCore<P> otherPort = (PortCore<P>) (posIsParent? negative : positive);

    addConnectedPort(parentPort == positive, parentPort, otherPort);
    if (posIsParent && negIsParent) {
      addConnectedPort(otherPort == positive, otherPort, parentPort);
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
    if (port.getOwner() != parentComponent) {
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

    return this;
  }

  <P extends PortType> TestCase trigger(
          KompicsEvent event, PortCore<P> port) {
    // register state
    fsm.addState(new Trigger(event, port));
    return this;
  }

  void check() {
    fsm.start();
    scheduler.shutdown();
  }
}
