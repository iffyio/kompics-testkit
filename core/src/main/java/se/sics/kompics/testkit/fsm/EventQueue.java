package se.sics.kompics.testkit.fsm;

import se.sics.kompics.KompicsEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EventQueue {

  private final ConcurrentLinkedQueue<KompicsEvent> q = new ConcurrentLinkedQueue<>();

  public void offer(KompicsEvent event) {
    q.offer(event);
    synchronized (this) {
      this.notifyAll();
    }
  }

  public KompicsEvent poll() {
    return q.poll();
  }

  public KompicsEvent peek() {
    return q.peek();
  }
}
