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

import com.google.common.base.Function;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

import java.util.ArrayList;
import java.util.List;

class ExpectMapper implements MultiEventSpec{

  final List<MapperStruct> expected = new ArrayList<MapperStruct>();
  private int mapperIndex = 0;

  private ComponentCore proxyComponent;

  private int numExpectedEvents;
  private Class<? extends KompicsEvent> eventType;
  private Function<? extends KompicsEvent, ? extends KompicsEvent> currentMapper;

  ExpectMapper(ComponentCore proxyComponent) {
    this.proxyComponent = proxyComponent;
  }

  <E extends KompicsEvent, R extends KompicsEvent> void setMapperForNext(
          int numExpectedEvents, Class<E> eventType, Function<E, R> mapper) {
    if (this.numExpectedEvents != 0) {
      throw new IllegalStateException(this.numExpectedEvents + " events have not yet been specified");
    }
    if (numExpectedEvents <= 0) {
      throw new IllegalArgumentException("number of expected events (" + numExpectedEvents + ") must be positive");
    }

    this.numExpectedEvents = numExpectedEvents;
    this.eventType = eventType;
    currentMapper = mapper;
  }

  void addExpectedEvent(
          Port<? extends PortType> listenPort, Port<? extends PortType> responsePort) {
    if (numExpectedEvents <= 0) {
      throw new IllegalStateException("no mapper was specified");
    }

    numExpectedEvents--;
    addNewMapperStruct(eventType, listenPort, responsePort, currentMapper);
  }

  <E extends KompicsEvent, R extends KompicsEvent> void addExpectedEvent(
          Class<E> eventType, Port<? extends PortType> listenPort,
          Port<? extends PortType> responsePort, Function<E, R> mapper) {
    addNewMapperStruct(eventType, listenPort, responsePort, mapper);
  }

  @Override
  public boolean match(EventSpec receivedSpec) {
    MapperStruct nextMapper = expected.get(mapperIndex);

    if (nextMapper.map(receivedSpec)) {
      mapperIndex++;
    } else {
      return false;
    }

    // messages are not handled until all seen (optionally?)
    if (mapperIndex == expected.size()) {
      for (MapperStruct mapper : expected) {
        mapper.handle();
      }
      mapperIndex = 0;
    }
    return true;
  }

  @Override
  public boolean isComplete() {
    return mapperIndex == 0;
  }

  private void addNewMapperStruct(
          Class<? extends KompicsEvent> eventType, Port<? extends PortType> listenPort,
          Port<? extends PortType> responsePort,
          Function<? extends KompicsEvent, ? extends KompicsEvent> mapper) {
    MapperStruct mapperStruct = new MapperStruct(eventType, mapper, listenPort, responsePort);
    expected.add(mapperStruct);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ExpectWithMapper<Seen(");
    int i = 0;
    for (; i < mapperIndex; i++) {
      MapperStruct m = expected.get(i);
      sb.append(" ").append(m);
    }

    sb.append(")Pending(");
    for (; i < expected.size(); i++) {
      MapperStruct m = expected.get(i);
      sb.append(" ").append(m);
    }
    sb.append(")>");
    return sb.toString();
  }

  private class MapperStruct {
    final Class<? extends KompicsEvent> eventType;
    final Function<? extends KompicsEvent, ? extends KompicsEvent> mapper;
    final Port<? extends PortType> listenPort;
    final Port<? extends PortType> responsePort;

    EventSpec receivedSpec;

    MapperStruct(Class<? extends KompicsEvent> eventType,
                 Function<? extends KompicsEvent, ? extends KompicsEvent> mapper,
                 Port<? extends PortType> listenPort,
                 Port<? extends PortType> responsePort) {
      this.eventType = eventType;
      this.mapper = mapper;
      this.listenPort = listenPort;
      this.responsePort = responsePort;
    }

    boolean map(EventSpec receivedSpec) {
      if (eventType.isAssignableFrom(receivedSpec.getEvent().getClass())) {
        this.receivedSpec = receivedSpec;
        return true;
      }
      return false;
    }

    void handle() {
      KompicsEvent receivedEvent = receivedSpec.getEvent();
      handleHelper(receivedEvent, mapper);
    }

    <E extends KompicsEvent, R extends KompicsEvent> void handleHelper(
            KompicsEvent event, Function<E, R> mapper) {
      KompicsEvent response = mapper.apply((E) event);
      //// TODO: 4/1/17 fail noisily
      if (response != null) {
        responsePort.doTrigger(response, 0, proxyComponent);
      }
    }

    @Override
    public String toString() {
      return eventType.getSimpleName();
    }
  }

}
