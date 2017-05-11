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
package se.sics.kompics.testkit.urb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.testkit.fd.TAddress;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UrbComponent extends ComponentDefinition {

  static final Logger logger = LoggerFactory.getLogger("Testkit");

  public static Set<TAddress> nodes;
  public static Map<TAddress, String> names;

  private final int MAJORITY;
  private final TAddress self;
  private final Set<Data> delivered = new HashSet<>();
  private final Set<Data> pending = new HashSet<>();

  private final Map<Data, Set<TAddress>> ack = new HashMap<>();

  Negative<UrbPort> urbPort = provides(UrbPort.class);
  Positive<Network> network = requires(Network.class);

  public UrbComponent(Init init) {
    self = init.self;
    MAJORITY = nodes.size();// / 2 + 1;
  }

  private Handler<URBTimeout> urbTimeoutHandler = new Handler<URBTimeout>() {
    @Override
    public void handle(URBTimeout event) {
      logger.warn("urb timeout");
      tryDeliver();
    }
  };

  private Handler<UrbBroadcast> urbHandler = new Handler<UrbBroadcast>() {
    @Override
    public void handle(UrbBroadcast urbBroadcast) {
      // clone counter
      Counter c = urbBroadcast.counter;
      c = new Counter(c);
      Data data = new Data(self, c);

      pending.add(data);
      ack.put(data, new HashSet<TAddress>());
      bebBroadcast(data);
    }
  };

  private Handler<BebMsg> bebHandler = new Handler<BebMsg>() {
    @Override
    public void handle(BebMsg bebMsg) {
      Data data = bebMsg.data;

      if (!ack.containsKey(data)) {
        ack.put(data, new HashSet<TAddress>());
      }

      logger.debug("{}: received msg {}, from {}, sender {}",
              names.get(self), data, names.get(bebMsg.getSource()),
              names.get(data.sender));

      Set<TAddress> acksForM = ack.get(data);
      acksForM.add(bebMsg.getSource());

      if (!pending.contains(data)) {
        pending.add(data);
        logger.debug("{}, acked {}, from {}, sender {}", names.get(self), data, names.get(bebMsg.getSource()), names.get(data.sender));
        assert !names.get(self).contains("p");
        bebBroadcast(data); // acknowledge seen
      }

      tryDeliver();
    }
  };

  private void tryDeliver() {
    for (Data data : pending) {
      if (ack.get(data).size() >= MAJORITY) {
        trigger(new UrbDeliver(data.msg), urbPort);
        delivered.add(data);
      }
    }

    for (Data data : delivered) {
      pending.remove(data);
    }
    if (names.get(self).contains("p")) {
      logger.warn("{} -> delivered total {} messages, {} pending",
              names.get(self), delivered.size(), pending.size());
    }
  }

  private void bebBroadcast(Data data) {
    for (TAddress dst : nodes) {
      BebMsg bebMsg = new BebMsg(
              self, dst, Transport.TCP, data);
      trigger(bebMsg, network);
    }
  }

  {
    subscribe(bebHandler, network);
    subscribe(urbHandler, urbPort);
  }

  public static class URBTimeout extends Timeout {
    public URBTimeout(SchedulePeriodicTimeout spt) {
      super(spt);
    }
  }

  public static class Init extends se.sics.kompics.Init<UrbComponent> {
    TAddress self;
    public Init(TAddress self) {
      this.self = self;
    }
  }
}
