package se.sics.kompics.testkit.urb;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class UrbDeliver implements KompicsEvent{

  public final Serializable message;

  public UrbDeliver(Serializable message) {
    this.message = message;
  }
}
