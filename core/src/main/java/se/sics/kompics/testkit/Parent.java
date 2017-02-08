package se.sics.kompics.testkit;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;

public class Parent extends ComponentDefinition{

  private final ComponentDefinition cut;

  <T extends ComponentDefinition>
  Parent(Class<T> cutClass, Init<T> initEvent) {
    // // TODO: 2/8/17 nosuchmethodexception with initEvent when Init.NONE
    //cut = create(cutClass, initEvent).getComponent();
    cut = create(cutClass, Init.NONE).getComponent();
  }

  ComponentDefinition getCut() {
    return cut;
  }

  public <T extends ComponentDefinition> Component createNewSetupComponent(Class<T> cClass, Init<T> initEvent) {
    return create(cClass, initEvent);
  }

  public <T extends ComponentDefinition> Component createNewSetupComponent(Class<T> cClass, Init.None initEvent) {
    return create(cClass, initEvent);
  }
}
