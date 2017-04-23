package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import se.sics.kompics.Fault;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

class FaultSpec implements Spec {

  private final Class<? extends Throwable> exceptionType;
  private final Predicate<Throwable> exceptionPredicate;
  private final Port<? extends PortType> port;
  Direction direction = Direction.OUTGOING;

  FaultSpec(
      Port<? extends PortType> port, Class<? extends Throwable> exceptionType) {
    this.exceptionType = exceptionType;
    this.exceptionPredicate = null;
    this.port = port;
  }

  FaultSpec(
      Port<? extends PortType> port, Predicate<Throwable> exceptionPredicate) {
    this.exceptionPredicate = exceptionPredicate;
    this.exceptionType = null;
    this.port = port;
  }

  @Override
  public boolean match(EventSpec receivedSpec) {
    if (!(receivedSpec.getEvent() instanceof Fault)) {
      return false;
    }
    // // TODO: 4/22/17 log error messages in case of failure
    Fault fault = (Fault) receivedSpec.getEvent();
    Throwable exception = fault.getCause();
    if (exceptionType != null) {
      return exceptionType.isAssignableFrom(fault.getCause().getClass());
    } else {
      assert exceptionPredicate != null;
      return exceptionPredicate.apply(exception);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof FaultSpec) {
      FaultSpec other = (FaultSpec) o;
      return exceptionType != null && other.exceptionType != null
             && exceptionType == other.exceptionType;
    }
    if (o instanceof EventSpec) {
      EventSpec eventSpec = (EventSpec) o;
      return this.match(eventSpec);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 31 * port.hashCode();
    result = 31 * result + direction.hashCode();
    return result;
  }
}
