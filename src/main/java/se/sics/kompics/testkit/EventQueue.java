package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

class EventQueue {

  private final ConcurrentLinkedQueue<EventSpec<? extends KompicsEvent>> q = new ConcurrentLinkedQueue<EventSpec<? extends KompicsEvent>>();

  synchronized void offer(EventSpec<? extends KompicsEvent> event) {
    q.offer(event);
    this.notifyAll();
  }

  synchronized EventSpec<? extends KompicsEvent> poll() {
    while (q.peek() == null) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return q.poll();
  }
}
