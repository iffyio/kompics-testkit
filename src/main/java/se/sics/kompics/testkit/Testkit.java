package se.sics.kompics.testkit;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;

public class Testkit {

  public static final Logger logger = LoggerFactory.getLogger("Testkit");

  private Testkit() {}

  public static <T extends ComponentDefinition> TestContext<T> newTestContext(
         Class<T> definition, Init<T> initEvent) {
    checkNotNull(definition, initEvent);
    return new TestContext<T>(definition, initEvent);
  }

  public static <T extends ComponentDefinition> TestContext<T> newTestContext(
          Class<T> definition, Init.None initEvent) {
    checkNotNull(definition, initEvent);
    return new TestContext<T>(definition, initEvent);
  }

  static void checkNotNull(Object... objects) {
    for (Object o : objects) {
      Preconditions.checkNotNull(o);
    }
  }

}
