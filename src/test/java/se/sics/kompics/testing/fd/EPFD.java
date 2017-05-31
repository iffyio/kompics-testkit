/**
 * This file is part of the Kompics Testing runtime.
 *
 * Copyright (C) 2017 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2017 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.testing.fd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.*;

import java.util.HashSet;
import java.util.UUID;

public class EPFD extends ComponentDefinition {

  private static final Logger logger = LoggerFactory.getLogger(EPFD.class);
  private TAddress self;
  private HashSet<TAddress> nodes, alive, suspected;
  private long delay = 500;

  private Negative<EPFDPort> epfd = provides(EPFDPort.class);
  private Positive<Network> network = requires(Network.class);
  private Positive<Timer> timer = requires(Timer.class);

  private UUID timerId;


  public EPFD() {
    init();
  }

  private void init() {
    self = Util.getEPFDAddr();
    nodes = new HashSet<>();
  }

  private Handler<Start> startHandler = new Handler<Start>() {
    @Override
    public void handle(Start start) {
      alive = new HashSet<>(nodes);
      suspected = new HashSet<>();
      logger.warn("started epfd on {}", self);

      set_timer();
    }
  };


  private Handler<PingTimeout> timeoutHandler = new Handler<PingTimeout>() {
    @Override
    public void handle(PingTimeout pingTimeout) {
      logger.warn("EPFD: timeout received");
/*      HashSet<TAddress> resurrected_nodes = new HashSet<>(alive);
      resurrected_nodes.retainAll(alive);*/

      for (TAddress node : nodes) {
        if (!suspected.contains(node) && !alive.contains(node)) {
          suspected.add(node);
          logger.warn("EPFD: Suspecting {}", node);
          trigger(new Suspect(node), epfd);
        }
        else if (alive.contains(node) && suspected.contains(node)){
          suspected.remove(node);
          logger.warn("EPFD: Restoring {}", node);
          trigger(new Restore(node), epfd);
        } else if (suspected.contains(node)) {
          logger.warn("EPFD: (Re)Suspecting {}", node);
          trigger(new Suspect(node), epfd);
        }

        trigger(new Ping(self, node), network);
      }
      alive = new HashSet<>();
      set_timer();
    }
  };

  private void set_timer() {
    ScheduleTimeout st = new ScheduleTimeout(delay);
    PingTimeout timeout = new PingTimeout(st);
    st.setTimeoutEvent(timeout);
    trigger(st, timer);
    timerId = timeout.getTimeoutId();
  }

  public void tearDown() {
    trigger(new CancelPeriodicTimeout(timerId), timer);
  }

  Handler<Pong> pongHandler = new Handler<Pong>() {
    @Override
    public void handle(Pong pong) {
      logger.warn("[{}] heartbeat reply from {}", self, pong.getSource());
      alive.add(pong.getSource());
    }
  };

  private Handler<Watch> watchHandler = new Handler<Watch>() {
    @Override
    public void handle(Watch event) {
      nodes.add(event.node);
      alive.add(event.node);
      logger.warn("now watching {}", event.node);
    }
  };

  public static class PingTimeout extends Timeout {
    public PingTimeout(ScheduleTimeout st) {
      super(st);
    }
  }

  public boolean equals(Object o) {
    return o instanceof PingTimeout;
  }

  public int hashCode() {
    return getClass().hashCode();
  }

  {
    subscribe(startHandler, control);
    subscribe(timeoutHandler, timer);
    //subscribe(pingHandler, network);
    subscribe(pongHandler, network);
    subscribe(watchHandler, epfd);
  }
}