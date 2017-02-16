package se.sics.kompics.testkit;

import se.sics.kompics.Port;
import se.sics.kompics.PortCore;
import se.sics.kompics.PortType;

import java.util.HashMap;
import java.util.Map;

class PortConfig {
  private final Map<Port<? extends PortType>, PortStructure<? extends PortType>> portStructure;
  private final Proxy parent;

  PortConfig(Proxy parent) {
    this.parent = parent;
    portStructure = new HashMap<>();
  }


  <P extends PortType> PortStructure<P> create(Port<P> port, boolean isPositive) {
    PortStructure<P> portStruct = new PortStructure<>(parent, port, isPositive);
    addPortStruct(port, portStruct);
    return portStruct;
  }

  <P extends PortType> PortStructure<P> create(Port<P> port) {
    PortStructure<P> portStruct = new PortStructure<>(parent, port);
    addPortStruct(port, portStruct);
    return portStruct;
  }

  <P extends PortType> PortStructure<P> get(Port<P> port) {
    return getPortStruct(port);
  }

  <P extends PortType> PortStructure<P> getOrCreate(PortCore<P> port, boolean isPositive) {
    PortStructure<P> portStruct = getPortStruct(port);
    if (portStruct == null) {
      portStruct = create(port, isPositive);
    }
    return portStruct;
  }
  private <P extends PortType> PortStructure<P> getPortStruct(Port<P> port) {
    return (PortStructure<P>) portStructure.get(port);
  }

  private <P extends PortType> void addPortStruct(
          Port<P> port, PortStructure<P> portStruct) {
    portStructure.put(port, portStruct);
  }

}
