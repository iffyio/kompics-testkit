package se.sics.kompics.testkit.fsm;

abstract class State {

  private StateTable stateTable;

  void setStateTable(StateTable stateTable) {
    this.stateTable = stateTable;
  }

  StateTable getStateTable() {
    return stateTable;
  }

  protected abstract boolean run();
}
