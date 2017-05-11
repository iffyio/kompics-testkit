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

import com.google.common.base.Predicate;
import org.junit.Test;
import se.sics.kompics.*;

import static junit.framework.Assert.assertEquals;

public class InspectTest {

  private TestContext<Pinger> tc = Testkit.newTestContext(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private Direction incoming = Direction.INCOMING;
  private Direction outgoing = Direction.OUTGOING;
  private static Ping ping = new Ping();
  private static Pong pong = new Pong();

  @Test
  public void work() {
    tc.connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class));
    tc.body().
       repeat(100).body().
        expect(ping, pinger.getNegative(PingPongPort.class), outgoing).
        inspect(expectedPings).
        expect(pong, pinger.getNegative(PingPongPort.class), incoming).
        inspect(expectedPongs).
       end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  private Predicate<Pinger> expectedPings = new Predicate<Pinger>() {
    int expectedPingsSent = 0;
    @Override
    public boolean apply(Pinger pinger) {
      return pinger.pingsSent == ++expectedPingsSent;
    }
  };

  private Predicate<Pinger> expectedPongs = new Predicate<Pinger>() {
    int expectedPongsReceived = 0;
    @Override
    public boolean apply(Pinger pinger) {
      return pinger.pongsReceived == ++expectedPongsReceived;
    }
  };

  public static class Pinger extends ComponentDefinition {
    Positive<PingPongPort> ppPort = requires(PingPongPort.class);
    int pingsSent = 0;
    int pongsReceived = 0;

    Handler<Pong> pongHandler = new Handler<Pong>() {
      @Override
      public void handle(Pong pong) {
        trigger(ping, ppPort);
        pingsSent++;
        pongsReceived++;
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) {
        trigger(ping, ppPort);
        pingsSent++;
      }
    };

    {
      subscribe(pongHandler, ppPort);
      subscribe(startHandler, control);
    }
  }

  public static class Ponger extends ComponentDefinition {
    Negative<PingPongPort> pingPongPort = provides(PingPongPort.class);

    Handler<Ping> pingHandler = new Handler<Ping>() {
      @Override
      public void handle(Ping ping) {
        trigger(pong, pingPongPort);
      }
    };

    {
      subscribe(pingHandler, pingPongPort);
    }
  }

  public static class PingPongPort extends PortType {
    {
      request(Ping.class);
      indication(Pong.class);
    }
  }

  public static class Ping implements KompicsEvent{ }
  public static class Pong implements KompicsEvent{ }
}
