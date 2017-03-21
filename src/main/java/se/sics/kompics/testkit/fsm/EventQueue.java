package se.sics.kompics.testkit.fsm;

import se.sics.kompics.KompicsEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EventQueue {

  private final ConcurrentLinkedQueue<EventSpec<? extends KompicsEvent>> q = new ConcurrentLinkedQueue<EventSpec<? extends KompicsEvent>>();

  public synchronized void offer(EventSpec<? extends KompicsEvent> event) {
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
