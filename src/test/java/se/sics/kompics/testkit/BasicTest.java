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

import org.junit.Test;
import se.sics.kompics.Component;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.testkit.pingpong.Ping;
import se.sics.kompics.testkit.pingpong.PingComparator;
import se.sics.kompics.testkit.pingpong.PingPongPort;
import se.sics.kompics.testkit.pingpong.Pinger;
import se.sics.kompics.testkit.pingpong.Pong;
import se.sics.kompics.testkit.pingpong.PongComparator;
import se.sics.kompics.testkit.pingpong.Ponger;

import static junit.framework.Assert.assertEquals;

public class BasicTest {

  private TestContext<Pinger> tc = Testkit.newTestContext(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component ponger = tc.create(Ponger.class, Init.NONE);
  private Direction incoming = Direction.INCOMING;
  private Direction outgoing = Direction.OUTGOING;
  private Negative<PingPongPort> pingerPort = pinger.getNegative(PingPongPort.class);
  private Positive<PingPongPort> pongerPort = ponger.getPositive(PingPongPort.class);

  @Test
  public void dropAllowDisallowTest() {
    tc.connect(pingerPort, pongerPort);
    tc.
      addComparator(Pong.class, new PongComparator()).
      addComparator(Ping.class, new PingComparator()).
      disallow(pong(0), pingerPort, outgoing).
      allow(pong(0), pingerPort, incoming).
      body().
            trigger(ping(0), pongerPort).
            expect(ping(1), pingerPort, outgoing).
            expect(pong(1), pingerPort, incoming).
      repeat(1).
        disallow(pong(3), pingerPort, incoming).
        drop(ping(2), pingerPort, outgoing).
        drop(ping(4), pingerPort, outgoing). // last msg on inner repeat
        body().
            trigger(ping(1), pingerPort.getPair()).
            repeat(1).
              allow(ping(4), pingerPort, outgoing).
              allow(pong(4), pingerPort, incoming).
              drop(ping(5), pingerPort, outgoing).
              body().
              trigger(ping(4), pingerPort.getPair()).
              expect(ping(1), pingerPort, outgoing).
              expect(pong(1), pingerPort, incoming).

              expect(ping(2), pingerPort, outgoing). // expect dropped msg
              expect(pong(2), pingerPort, incoming).

              expect(ping(3), pingerPort, outgoing).
              expect(pong(3), pingerPort, incoming).
            end().
        trigger(ping(1), pingerPort.getPair()).
        expect(ping(1), pingerPort, outgoing).
        expect(pong(1), pingerPort, incoming).
        expect(ping(2), pingerPort, outgoing).
      end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  private Ping ping(int i) {
    return new Ping(i);
  }

  private Pong pong(int i) {
    return new Pong(i);
  }

}
