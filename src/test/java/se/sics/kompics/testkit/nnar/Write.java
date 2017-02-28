package se.sics.kompics.testkit.nnar;

import se.sics.kompics.KompicsEvent;

class Write implements KompicsEvent{
  final int val;
  Write(int val) {
    this.val = val;
  }
}
