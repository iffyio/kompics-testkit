package se.sics.kompics.testkit;

import com.google.common.base.Preconditions;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;

public class TestKit {

  private TestKit() {}

  public static <T extends ComponentDefinition> TestContext newTestCase(
         Class<T> definition, Init<T> initEvent) {
    newTestCasePreconditions(definition, initEvent);
    return new TestContext(definition, initEvent);
  }

  public static <T extends ComponentDefinition> TestContext newTestCase(
          Class<T> definition, Init.None initEvent) {
    newTestCasePreconditions(definition, initEvent);
    return new TestContext(definition, initEvent);
  }

  private static void newTestCasePreconditions(
          Class definition, Init initEvent) {
    Preconditions.checkNotNull(definition);
    Preconditions.checkNotNull(initEvent);
  }

}
