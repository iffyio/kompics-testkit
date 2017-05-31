package se.sics.kompics.testkit.fd;

import se.sics.kompics.Kompics;

public class Main {

  public static void main(String... a) {
    Kompics.createAndStart(Watcher.class);
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Kompics.shutdown();
  }
}
