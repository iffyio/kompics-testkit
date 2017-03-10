package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import se.sics.kompics.Fault;

public class ExpectedFault {

  private enum STATUS {
    PENDING, SUCCESS, FAILURE
  }

  private final Class<? extends Throwable> exceptionType;
  private final Predicate<Throwable> exceptionPredicate;
  private final Fault.ResolveAction resolveAction;

  private STATUS status;
  private String message;

  public ExpectedFault(
          Class<? extends Throwable> exceptionType, Fault.ResolveAction resolveAction) {
    this.exceptionType = exceptionType;
    this.resolveAction = resolveAction;
    this.exceptionPredicate = null;
    reset();
  }

  public ExpectedFault(
          Predicate<Throwable> exceptionPredicate, Fault.ResolveAction resolveAction) {
    this.exceptionPredicate = exceptionPredicate;
    this.resolveAction = resolveAction;
    this.exceptionType = null;
    reset();
  }

  synchronized Fault.ResolveAction matchFault(Fault fault) {
    if (status != STATUS.PENDING) {
      // unexpected fault was thrown before previous exception was asserted
      return Fault.ResolveAction.ESCALATE;
    }

    if (exceptionType != null) {
      matchByType(fault.getCause());
    } else {
      matchByPredicate(fault.getCause());
    }

    this.notifyAll();
    return resolveAction;
  }

  public synchronized Result getResult() {
    while (status == STATUS.PENDING) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    Result r = new Result(status == STATUS.SUCCESS, getMessage());
    reset();
    return r;
  }

  private void reset() {
    status = STATUS.PENDING;
    message = null;
  }

  private String getMessage() {
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
    status = STATUS.SUCCESS;
    message = "Expect Fault Succeeded (" + exception + ")";
  }

  private void fail(String message) {
    status = STATUS.FAILURE;
    this.message = "Expect Fault Failed: " + message;
  }

  public static class Result {
    public final boolean succeeded;
    public final String message;
    private Result(boolean succeeded, String message) {
      this.succeeded = succeeded;
      this.message = message;
    }
  }
}
