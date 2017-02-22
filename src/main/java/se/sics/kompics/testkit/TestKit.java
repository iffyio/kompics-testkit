package se.sics.kompics.testkit;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;

public class TestKit {

  public static <T extends ComponentDefinition> TestCase newTestCase(
         Class<T> cutClass, Init initEvent) {
    return new TestCase(cutClass, initEvent);
  }

}
