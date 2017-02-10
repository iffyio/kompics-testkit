package se.sics.kompics.testkit.fsm;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EventQueue {

  private final ConcurrentLinkedQueue<EventSpec> q = new ConcurrentLinkedQueue<>();

  public synchronized void offer(EventSpec event) {
    q.offer(event);
    this.notifyAll();
  }

  public synchronized EventSpec poll() {
    while (q.peek() == null) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return q.poll();
  }
}
