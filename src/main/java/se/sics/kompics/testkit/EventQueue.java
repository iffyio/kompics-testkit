package se.sics.kompics.testkit;

import java.util.concurrent.ConcurrentLinkedQueue;

class EventQueue {

  private final ConcurrentLinkedQueue<EventSpec> q = new ConcurrentLinkedQueue<EventSpec>();

  synchronized void offer(EventSpec event) {
    q.offer(event);
    this.notifyAll();
  }

  synchronized EventSpec poll() {
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
