package se.sics.kompics.testkit;

import se.sics.kompics.*;

import java.util.*;

class PortConfig {
  private final Map<Port<? extends PortType>, PortStructure> positivePortStructures = new HashMap<>();
  private final Map<Port<? extends PortType>, PortStructure> negativePortStructures = new HashMap<>();
  private final Proxy proxy;

  PortConfig(Proxy proxy) {
    this.proxy = proxy;
    setupPortStructures();
  }

  private void setupPortStructures() {
    setupPortStructures(true);  // provided ports
    setupPortStructures(false); // required ports
  }

  private void setupPortStructures(boolean provided) {
    Map<Class<? extends PortType>, JavaPort<? extends PortType>> ports =
            provided? proxy.getCutPositivePorts() : proxy.getCutNegativePorts();

    for (Class<? extends PortType> portType : ports.keySet()) {
      Port<? extends PortType> outboundPort = ports.get(portType);

      if (!isMonitoredPort(portType)) {
        continue;
      }

      Kompics.logger.warn("creating portStructure for {} {}",
              provided? "positive":"negative", portType.getSimpleName());

      Port<? extends PortType> inboundPort = provided?
              proxy.providePort(portType) : proxy.requirePort(portType);

      PortStructure portStructure = new PortStructure(proxy, inboundPort, outboundPort, provided);

      if (provided) {
        positivePortStructures.put(outboundPort, portStructure);
      } else {
        negativePortStructures.put(outboundPort, portStructure);
      }
    }
  }

  private boolean isMonitoredPort(Class<? extends PortType> portClass) {
    return !portClass.equals(LoopbackPort.class) &&
           !portClass.equals(ControlPort.class);
  }

  public <P extends PortType> void connectPorts(Positive<P> positive,
                                                Negative<P> negative,
                                                ChannelFactory factory) {

    boolean cutOwnsPositive = positive.getPair().getOwner() == proxy.getComponentUnderTest();
    boolean cutOwnsNegative = negative.getPair().getOwner() == proxy.getComponentUnderTest();

    PortCore<P> proxyPort = (PortCore<P>) (cutOwnsPositive? positive : negative);
    PortCore<P> otherPort = (PortCore<P>) (cutOwnsPositive? negative : positive);

    connectPort(proxyPort, otherPort, factory);

    if (cutOwnsPositive && cutOwnsNegative) {
      connectPort(otherPort, proxyPort, factory);
    }
  }

  private <P extends PortType> void connectPort(PortCore<P> proxyPort,
                                                PortCore<P> otherPort,
                                                ChannelFactory factory) {
    if (positivePortStructures.containsKey(proxyPort)) {
      positivePortStructures.get(proxyPort).addConnectedPort(otherPort, factory);
    } else {
      negativePortStructures.get(proxyPort).addConnectedPort(otherPort, factory);
    }
  }

}
