package se.sics.kompics.testkit;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class EventQueue {

  private long timeoutMS = 100;
  private final BlockingQueue<EventSpec> q = new LinkedBlockingQueue<EventSpec>();

  void setTimeout(long timeout) {
    if (timeout < 0) {
      throw new IllegalStateException("Negative timeout");
    }
    this.timeoutMS = timeout;
  }

  void offer(EventSpec event) {
    q.offer(event);
  }

  EventSpec poll() {
    try {
      return q.poll(timeoutMS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return null;
    }
  }
}
