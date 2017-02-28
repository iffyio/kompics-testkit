package se.sics.kompics.testkit.nnar;

import se.sics.kompics.KompicsEvent;

public class ReadReturn implements KompicsEvent{
  public final int val;
  public ReadReturn(Integer val) {
    this.val = val == null? 0 : val;
  }
}
