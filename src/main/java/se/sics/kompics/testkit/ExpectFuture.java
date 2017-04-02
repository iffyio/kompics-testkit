package se.sics.kompics.testkit;

import se.sics.kompics.ComponentCore;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ExpectFuture extends Spec{

  List<FutureStruct> expected = new ArrayList<FutureStruct>();
  Map<Future<? extends KompicsEvent, ? extends KompicsEvent>, FutureStruct> futures =
          new HashMap<Future<? extends KompicsEvent, ? extends KompicsEvent>, FutureStruct>();
  private List<FutureStruct> trigger = new ArrayList<FutureStruct>(); // trigger order
  private int futureIndex = 0;

  private ComponentCore proxyComponent;

  ExpectFuture(ComponentCore proxyComponent) {
    this.proxyComponent = proxyComponent;
  }

  <E extends KompicsEvent, R extends KompicsEvent> void addExpectedEvent(
          Class<E> eventType, Port<? extends PortType> listenPort, Future<E, R> future) {
    if (futures.containsKey(future)) {
      throw new IllegalArgumentException("Future (" + future + ") has used in previous expect");
    }

    FutureStruct futureStruct = new FutureStruct(eventType, listenPort, future);
    expected.add(futureStruct);
    futures.put(future, futureStruct);
  }

  void addTrigger(Port<? extends PortType> responsePort,
                  Future<? extends KompicsEvent, ? extends KompicsEvent> future) {
    if (!futures.containsKey(future)) {
      throw new IllegalArgumentException("Future used to trigger must be used in previous expect");
    }

    FutureStruct futureStruct = futures.remove(future);
    futureStruct.responsePort = responsePort;
    trigger.add(futureStruct);
  }

  @Override
  StateTable.Transition getTransition(EventSpec receivedSpec, int state) {
    FutureStruct nextFuture = expected.get(futureIndex);

    if (nextFuture.map(receivedSpec)) {
      futureIndex++;
    } else {
      return null;
    }

    int nextState = state;

    if (futureIndex == expected.size()) {
      for (FutureStruct futureStruct : trigger) {
        futureStruct.handle();
      }

      futureIndex = 0;
      nextState = state + 1;
    }

    return new StateTable.Transition(receivedSpec, Action.DROP, nextState);
  }

  private class FutureStruct {
    final Class<? extends KompicsEvent> eventType;
    final Port<? extends PortType> listenPort;
    Port<? extends PortType> responsePort;
    final Future<? extends KompicsEvent, ? extends KompicsEvent> future;

    FutureStruct(Class<? extends KompicsEvent> eventType, Port<? extends PortType> listenPort,
                 Future<? extends KompicsEvent, ? extends KompicsEvent> future) {
      this.eventType = eventType;
      this.listenPort = listenPort;
      this.future = future;
    }

    boolean map(EventSpec receivedSpec) {
      if (eventType.isAssignableFrom(receivedSpec.getEvent().getClass())) {
        mapHelper(receivedSpec, future);
        return true;
      }
      return false;
    }

    <E extends KompicsEvent, R extends KompicsEvent> void mapHelper(
            EventSpec receivedSpec, Future<E, R> future) {
      future.set((E) receivedSpec.getEvent());
    }

    void handle() {
      KompicsEvent response = future.get();
      //// TODO: 4/1/17 again, fail noisily
      if (response != null) {
        responsePort.doTrigger(response, 0, proxyComponent);
      }
    }
  }
}
