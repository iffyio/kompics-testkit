package se.sics.kompics.testkit.urb;

import java.io.Serializable;

public class Counter implements Serializable {
  public int i = 0;

  public Counter() { }

  public Counter(Counter other) {
    this.i = other.i;
  }

  public boolean equals(Object o) {
    return o instanceof Counter && i - ((Counter) o).i == 0;
  }

  public int hashCode() {
    return i;
  }

  public String toString() {
    return String.valueOf(i);
  }
}
