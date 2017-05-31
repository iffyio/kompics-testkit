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
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;

public class Watcher extends ComponentDefinition{
  private static final Logger logger = LoggerFactory.getLogger(Ponger.class);
  Positive<EPFDPort> epfdPort = requires(EPFDPort.class);
  TAddress epfdAddr, pongerAddr;
  public Watcher() {

    init();
    epfdAddr = Util.getEPFDAddr();
    pongerAddr = Util.getPongerAddr();

    Component timer = create(JavaTimer.class, Init.NONE);
    Component networkEPFD = create(NettyNetwork.class, new NettyInit(epfdAddr));
    Component networkPonger = create(NettyNetwork.class, new NettyInit(pongerAddr));
    Component ponger = create(Ponger.class, Init.NONE);
    Component epfd = create(EPFD.class, Init.NONE);


    connect(epfd.getNegative(Timer.class), timer.getPositive(Timer.class));
    connect(epfd.getNegative(Network.class), networkEPFD.getPositive(Network.class));

    connect(epfdPort.getPair(), epfd.getPositive(EPFDPort.class));
    connect(ponger.getNegative(Network.class), networkPonger.getPositive(Network.class));

  }

  private Handler<Start> startHandler = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      logger.info("watcher started! epfd at {}", epfdAddr);
      Watch w = new Watch(pongerAddr);
      trigger(w, epfdPort);
    }
  };

  private Handler<Restore> restoreHandler = new Handler<Restore>() {
    @Override
    public void handle(Restore event) {
      logger.info("restoring {}", event.node);
    }
  };
  private Handler<Suspect> suspectHandler = new Handler<Suspect>() {
    @Override
    public void handle(Suspect event) {
      logger.info("suspecting {}", event.node);
    }
  };

  private void init() {
    //self = createAddr("pingpong.fd.host", "pingpong.fd.port");
    subscribe(startHandler, control);
    subscribe(suspectHandler, epfdPort);
    subscribe(restoreHandler, epfdPort);
  }

}
