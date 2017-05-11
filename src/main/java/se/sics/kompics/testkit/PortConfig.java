/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
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

  private final Map<Port<? extends PortType>, PortStructure> portStructs = new HashMap<Port<? extends PortType>, PortStructure>();
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
  <P extends PortType> void doConnect(Positive<P> positive, Negative<P> negative, ChannelFactory factory) {

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

  private boolean isMonitoredPort(Class<? extends PortType> portClass) {
    return !(portClass.equals(LoopbackPort.class) || portClass.equals(ControlPort.class));
  }

}
