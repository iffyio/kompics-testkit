package se.sics.kompics.testkit;

import se.sics.kompics.ChannelFactory;
import se.sics.kompics.ControlPort;
import se.sics.kompics.JavaPort;
import se.sics.kompics.LoopbackPort;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortCore;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;

import java.util.HashMap;
import java.util.Map;


class PortConfig {
  private final Map<Port<? extends PortType>, PortStructure> portStructs = new HashMap<>();
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

      Port<? extends PortType> inboundPort =
              provided? proxy.providePort(portType) : proxy.requirePort(portType);

      PortStructure portStruct = new PortStructure(proxy, inboundPort, outboundPort, provided);

      portStructs.put(outboundPort, portStruct);
    }
  }

  private boolean isMonitoredPort(Class<? extends PortType> portClass) {
    return !portClass.equals(LoopbackPort.class) &&
           !portClass.equals(ControlPort.class);
  }

  public <P extends PortType> void doConnect(Positive<P> positive,
                                             Negative<P> negative,
                                             ChannelFactory factory) {

    boolean cutOwnsPositive = positive.getPair().getOwner() == proxy.getComponentUnderTest();
    boolean cutOwnsNegative = negative.getPair().getOwner() == proxy.getComponentUnderTest();

    PortCore<P> proxyPort = (PortCore<P>) (cutOwnsPositive? positive : negative);
    PortCore<P> otherPort = (PortCore<P>) (cutOwnsPositive? negative : positive);

    portStructs.get(proxyPort).addConnectedPort(otherPort, factory);

    if (cutOwnsPositive && cutOwnsNegative) {
      portStructs.get(otherPort).addConnectedPort(proxyPort, factory);
    }
  }

  boolean isConnectedPort(Port<? extends PortType> port) {
    PortStructure portStruct = portStructs.get(port);
    return !(portStruct == null || portStruct.getConnectedPorts().isEmpty());
  }
}
