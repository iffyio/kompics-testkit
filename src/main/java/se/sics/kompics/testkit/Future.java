package se.sics.kompics.testkit;

import se.sics.kompics.KompicsEvent;

abstract class Future<E extends KompicsEvent, R extends KompicsEvent> {

  public abstract void set(E request);
  public abstract R get();

  @Override
  public final int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public final boolean equals(Object o) {
    return this == o;
  }
}
