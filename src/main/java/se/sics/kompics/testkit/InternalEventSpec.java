package se.sics.kompics.testkit;

import com.google.common.base.Predicate;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.JavaComponent;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;

class InternalEventSpec implements Spec {


  private boolean inspect;
  private Predicate<? extends ComponentDefinition> inspectPredicate;
  private ComponentDefinition definitionUnderTest;

  private boolean trigger;
  private KompicsEvent event;
  private Port<? extends PortType> port;

  <T extends ComponentDefinition> InternalEventSpec(T definitionUnderTest, Predicate<T> inspectPredicate) {
    this.definitionUnderTest = definitionUnderTest;
    this.inspectPredicate = inspectPredicate;
    inspect = true;
  }

  InternalEventSpec(KompicsEvent event, Port<? extends PortType> port) {
    this.event = event;
    this.port = port;
    trigger = true;
  }

  @Override
  public boolean match(EventSpec receivedSpec) {
    return false;
  }

  String performInternalEvent() {
    if (inspect) {
      return doInspect();
    }
    if (trigger) {
      return doTrigger();
    }
    return null;
  }

  private String doTrigger() {
    Testkit.logger.debug("triggered({})\t", event);
    port.doTrigger(event, 0, port.getOwner());
    return null;
  }

  private String doInspect() {
    Testkit.logger.debug("Inspecting Component");
    JavaComponent cut = (JavaComponent) definitionUnderTest.getComponentCore();

    // // TODO: 3/31/17 do not poll
    while (cut.workCount.get() > 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    boolean successful = inspect(definitionUnderTest, inspectPredicate);

    return successful? null : "Component assertion failed";
  }

  private <T extends ComponentDefinition> boolean inspect(
      ComponentDefinition definitionUnderTest, Predicate<T> inspectPredicate) {
    return inspectPredicate.apply((T) definitionUnderTest);
  }

  @Override
  public String toString() {
    if (inspectPredicate != null) {
      return inspectPredicate.toString();
    }
    if (event != null) {
      return "trigger(" + event + ")";
    }
    return null;
  }
}
