package se.sics.kompics.testkit.fsm;

import se.sics.kompics.KompicsEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EventQueue {

  private final ConcurrentLinkedQueue<QueuedEvent> q = new ConcurrentLinkedQueue<>();

  public synchronized void offer(QueuedEvent event) {
    q.offer(event);
    this.notifyAll();
  }

  public synchronized QueuedEvent poll() {
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
