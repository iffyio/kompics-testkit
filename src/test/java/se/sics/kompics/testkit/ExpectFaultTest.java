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
import static junit.framework.Assert.assertEquals;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.Request;
import se.sics.kompics.Start;

import static se.sics.kompics.testkit.Direction.INCOMING;

public class ExpectFaultTest {

  private TestContext<Pinger> tc = Testkit.newTestContext(Pinger.class, Init.NONE);
  private Component pinger = tc.getComponentUnderTest();
  private Component ponger = tc.create(Ponger.class, Init.NONE);

  private Predicate<Throwable> negativePingPredicate = new Predicate<Throwable>() {
    @Override
    public boolean apply(Throwable throwable) {
      return throwable.getMessage().equals(NEGATIVE_PONG);
    }
  };

  private static int N = 5;
  private static String NEGATIVE_PONG = "negative ping";
  private static String MULTIPLE_OF_N = "multiple of " + N;
  private Ping ping = new Ping(0);
  private Pong pong = new Pong(0);

  private BlockInit incrementCounters = new BlockInit() {
    @Override
    public void init() {
      ping.count++;
      pong.count++;
    }
  };

  @Test
  public void faultPairedWithTriggerTest() {
    throwErrorOnNegativeFault(true);
  }

  @Test
  public void faultPairedWithExpectTest() {
    tc.
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      body().

        repeat(10).
        body().
          repeat(N - 1). // pinger receive n - 1 pongs
            onEachIteration(incrementCounters).
          body().
            trigger(pong, ponger.getPositive(PingPongPort.class).getPair()).
            expect(pong, pinger.getNegative(PingPongPort.class), INCOMING).
          end().

            // on Nth pong, throws exception
          repeat(1).
            onEachIteration(incrementCounters).
          body().
            trigger(pong, ponger.getPositive(PingPongPort.class).getPair()).
            expect(pong, pinger.getNegative(PingPongPort.class), INCOMING).
            expectFault(IllegalStateException.class, Fault.ResolveAction.IGNORE).
          end().
        end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  @Test
  public void expectFaultWithPredicateTest() {
    throwErrorOnNegativeFault(false);
  }


  private void throwErrorOnNegativeFault(boolean matchByClass) {
    Pong negativePong = new Pong(-1);

    tc.
      connect(pinger.getNegative(PingPongPort.class), ponger.getPositive(PingPongPort.class)).
      body().

      repeat(10, incrementCounters).
        body().
          trigger(pong, ponger.getPositive(PingPongPort.class).getPair()).
          expect(pong, pinger.getNegative(PingPongPort.class), INCOMING).

          // trigger from ponger's port
          trigger(negativePong, ponger.getPositive(PingPongPort.class).getPair()).
          expect(negativePong, pinger.getNegative(PingPongPort.class), INCOMING);
          matchNegativePong(matchByClass);

          // trigger directly on pinger's port
          tc.trigger(negativePong, pinger.getNegative(PingPongPort.class));
          matchNegativePong(matchByClass);
      tc.end();

    assertEquals(tc.check(), tc.getFinalState());
  }

  private void matchNegativePong(boolean matchByClass) {
    if (matchByClass) {
      tc.expectFault(IllegalStateException.class, Fault.ResolveAction.IGNORE);
    } else {
      tc.expectFault(negativePingPredicate, Fault.ResolveAction.IGNORE);
    }
  }

  public static class Pinger extends ComponentDefinition {

    Positive<PingPongPort> ppPort = requires(PingPongPort.class);

    Handler<Pong> pongHandler = new Handler<Pong>() {
      @Override
      public void handle(Pong pong) {
        if (pong.count < 0) {
          throw new IllegalStateException(NEGATIVE_PONG);
        }

        if (pong.count != 0 && (pong.count) % N == 0) {
          throw new IllegalStateException(MULTIPLE_OF_N);
        }
      }
    };

    Handler<Start> startHandler = new Handler<Start>() {
      @Override
      public void handle(Start event) { }
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
        Pong pong = new Pong(ping.count);
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

  static class Ping extends Request {
    int count;
    Ping(int count) { this.count = count; }

    public boolean equals(Object o) {
      return o instanceof Ping && ((Ping) o).count == count;
    }

    public int hashCode() {
      return count;
    }

    public String toString() {
      return "" + count;
    }
  }

  static class Pong implements KompicsEvent {

    int count;
    Pong(int count) { this.count = count; }

    public boolean equals(Object o) {
      return o instanceof Pong && ((Pong)o).count == count;
    }

    public int hashCode() {
      return count;
    }

    public String toString() {
      return "" + count;
    }
  }
}
