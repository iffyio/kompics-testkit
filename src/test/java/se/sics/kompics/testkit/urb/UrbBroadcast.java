package se.sics.kompics.testkit.urb;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class UrbBroadcast implements KompicsEvent {

  final Serializable message;

  public UrbBroadcast(Serializable message) {
    this.message = message;
  }
}
