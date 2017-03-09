package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import se.sics.kompics.Fault;

public class AssertThrown {

  private enum STATUS {
    PENDING, PASSED, FAILED
  }

  private final Class<? extends Throwable> exceptionType;
  private final Predicate<Throwable> exceptionPredicate;
  private final Fault.ResolveAction resolveAction;

  private STATUS status;
  private String message;

  public AssertThrown(
          Class<? extends Throwable> exceptionType, Fault.ResolveAction resolveAction) {
    this.exceptionType = exceptionType;
    this.resolveAction = resolveAction;
    this.exceptionPredicate = null;
    reset();
  }

  public AssertThrown(
          Predicate<Throwable> exceptionPredicate, Fault.ResolveAction resolveAction) {
    this.exceptionPredicate = exceptionPredicate;
    this.resolveAction = resolveAction;
    this.exceptionType = null;
    reset();
  }

  public synchronized Fault.ResolveAction matchException(Fault fault) {
    if (status != STATUS.PENDING) {
      throw new IllegalStateException("reset() must be called before reusing object");
    }

    if (exceptionType != null) {
      matchByType(fault.getCause());
    } else {
      matchByPredicate(fault.getCause());
    }

    if (status == STATUS.PASSED) {
      return resolveAction;
    } else {
      return Fault.ResolveAction.ESCALATE;
    }
  }

  public synchronized boolean isSuccessful() {
    while (status == STATUS.PENDING) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return status == STATUS.PASSED;
  }

  public void reset() {
    status = STATUS.PENDING;
    message = null;
  }

  public Fault.ResolveAction getResolveAction() {
    return resolveAction;
  }

  public String getMessage() {
    return status == STATUS.PENDING?
            String.format("Expected to throw exception matching '%s'" +
                          " but none was thrown", strReprOfExpectedException())
            : message;
  }

  public String strReprOfExpectedException() {
    return exceptionType != null? exceptionType.toString() : exceptionPredicate.toString();
  }


  private void matchByType(Throwable exception) {
    if (exceptionType.isAssignableFrom(exception.getClass())) {
      pass(exception);
    } else {
      fail(String.format("Could not match throwable of type %s, Actual %s",
              exceptionType, exception));
    }
  }

  private void matchByPredicate(Throwable exception) {
    if (exceptionPredicate.apply(exception)) {
      pass(exception);
    } else {
      fail("Predicate does not match exception\n" + exception);
    }
  }

  private void pass(Throwable exception) {
    status = STATUS.PASSED;
    message = "Asserted thrown\n" + exception;
  }

  private void fail(String message) {
    status = STATUS.FAILED;
    this.message = "AssertThrown Failed: " + message;
  }


}
