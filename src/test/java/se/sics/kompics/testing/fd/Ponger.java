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

public class Ponger extends ComponentDefinition {
  private static final Logger logger = LoggerFactory.getLogger(Ponger.class);

  Positive<Network> network = requires(Network.class);
  TAddress self;

  public Ponger() {
    init();
  }

  private void init() {
    self = Util.getPongerAddr();
  }

  int i = 0;
  private Handler<Ping> pingHandler = new Handler<Ping>() {
    @Override
    public void handle(Ping ping) {
      logger.warn("Ponger: received {}", ping);
      //if (i++ % 2 == 0)
      trigger(new Pong(self, ping.getSource()), network);
    }
  };

  private Handler<Start> startHandler = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      logger.warn("ponger started {}", self);
    }
  };

  {
    subscribe(startHandler, control);
    subscribe(pingHandler, network);
  }
}
