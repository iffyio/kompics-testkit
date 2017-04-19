package se.sics.kompics.testkit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

class Conditional {

  private final Conditional main;
  private final Set<Spec> input;
  private final Map<ID, Statement> globalStmts;

  private int stateIDs = 0;
  private int index = 0;
  private int numEitherItems = 0;
  private int numOrItems = 0;

  private Map<Integer, Statement> stmts = new HashMap<Integer, Statement>();
  private Map<Integer, Conditional> conditionals = new HashMap<Integer, Conditional>();

  private LinkedList<Statement> eitherBlock = new LinkedList<Statement>();
  private LinkedList<Statement> orBlock = new LinkedList<Statement>();
  private boolean inEitherBlock = true;
  final Conditional parent;

  private Statement firstStmt;
  private Statement finalStmt;

  private int numMergedStates;

  Conditional(Conditional parent) {
    if (parent == null) {
      input = new HashSet<Spec>();
      globalStmts = new HashMap<ID, Statement>();
      this.main = this;
      this.parent = null;
    } else {
      this.main = parent.main;
      this.parent = parent;
      this.input = main.input;
      this.globalStmts = main.globalStmts;
    }
  }

  private int nextid() {
    return isMain()? stateIDs++ : main.nextid();
  }

  void addChild(EventSpec eventSpec) {
    Statement stmt = new Statement(main.nextid(), eventSpec);
    stmts.put(index++, stmt);

    if (inEitherBlock) {
      numEitherItems++;
    } else {
      numOrItems++;
    }
  }

  void addChild(Conditional child) {
    conditionals.put(index++, child);

    if (inEitherBlock) {
      numEitherItems++;
    } else {
      numOrItems++;
    }
  }

  void or() {
    if (!inEitherBlock) {
      throw new IllegalStateException("or can only be called once for a conditional");
    }
    inEitherBlock = false;
  }

  int resolve(int startStateNum, StateTable table, Block block, Spec nullSpec) {
    if (!isMain()) {
      return main.resolve(startStateNum, table, block, nullSpec);
    } else {
      Testkit.logger.error("resolving...");
      Statement finalStmt = new Statement(nextid(), nullSpec);
      finalStmt.addTransition(nullSpec, finalStmt);
      resolve(finalStmt);
      return convert(startStateNum, table, block);
    }
  }

  private int convert(int startState, StateTable table, Block block) {
    int nextAvailState = startState;

    Map<Statement, Integer> stateMap = new HashMap<Statement, Integer>();
    stateMap.put(firstStmt, nextAvailState++);
    stateMap.put(finalStmt, startState + numMergedStates); // return final state id is next available state
    LinkedList<Statement> pending = new LinkedList<Statement>(Collections.singletonList(firstStmt));
    HashSet<Statement> visited = new HashSet<Statement>();

    while (!pending.isEmpty()) {
      Statement currentStmt = pending.removeFirst();
      visited.add(currentStmt);
      visited.add(finalStmt);
      int currentState = stateMap.get(currentStmt);
      for (Map.Entry<Spec, Statement> entry : currentStmt.transitions.entrySet()) {
        Spec spec = entry.getKey();
        Statement nextStmt = entry.getValue();

        if (!(pending.contains(nextStmt) || visited.contains(nextStmt))) {
          pending.add(nextStmt);
        }
        Integer nextState = stateMap.get(nextStmt);
        if (nextState == null) {
          nextState = nextAvailState++;
          stateMap.put(nextStmt, nextState);
        }
        table.addTransition(currentState, nextState, spec, block);
      }
    }
    // all stmts were mapped
    assert nextAvailState - startState == numMergedStates - 1;
    Testkit.logger.error("map: {}", stateMap);
    return stateMap.get(finalStmt);
  }

  private void resolve(Statement localFinalStmt) {
    assert firstStmt == null;
    assert finalStmt == null;
    finalStmt = localFinalStmt;

    resolve(0, numEitherItems, eitherBlock);
    resolve(numEitherItems, numOrItems, orBlock);
    merge();
    print();
    Testkit.logger.error("either {}", eitherBlock);
    Testkit.logger.error("or {}", orBlock);
  }

  private void resolve(int start, int numItems, LinkedList<Statement> blockStmts) {
    Statement latest = null;
    Statement next;
    Statement current;
    for (int i = start + numItems - 1; i >= start; i--) {
      next = latest == null? finalStmt : latest;
      if (stmts.containsKey(i)) {
        current = stmts.get(i);
        current.addTransition(current.spec, next);
      } else {
        Conditional child = conditionals.get(i);
        child.resolve(next);
        current = child.firstStmt;
      }
      latest = current;
      blockStmts.addFirst(current);
    }
  }

  private void merge() {
    Statement startStmt = new Statement(
        Arrays.asList(eitherBlock.getFirst(), orBlock.getFirst()));
    firstStmt = startStmt;
    Testkit.logger.error("Startstate: {}", startStmt);

    Set<Statement> visited = new HashSet<Statement>();
    LinkedList<Statement> pending = new LinkedList<Statement>(Collections.singleton(startStmt));

    while (!pending.isEmpty()) {
      Statement currentStmt = pending.removeFirst();
      visited.add(currentStmt);

      for (Spec spec : input) {
        HashSet<Statement> reachable = new HashSet<Statement>();
        for (Statement child : currentStmt.children) {
          Statement r = child.getNextStateOn(spec);
          if (r != null) {
            reachable.add(r);
          }
        }
        if (!reachable.isEmpty()) {
          Statement nextStmt = new Statement(reachable);
          if (globalStmts.containsKey(nextStmt.id)) {
            nextStmt = globalStmts.get(nextStmt.id);
          }

          currentStmt.addTransition(spec, nextStmt);
          if (!(visited.contains(nextStmt) || pending.contains(nextStmt))) {
            pending.add(nextStmt);
          }
        }
      }
    }

    numMergedStates = visited.size();
  }

  void end() {
    if (numEitherItems == 0) {
      throw new IllegalStateException("Either block may not be empty");
    }
    if (numOrItems == 0) {
      throw new IllegalStateException("Or block may not be empty");
    }
  }

  private void print() {
    LinkedList<Statement> pending = new LinkedList<Statement>();
    Set<Statement> visited = new HashSet<Statement>();
    pending.add(firstStmt);

    while (!pending.isEmpty()) {
      Statement currentStmt = pending.removeFirst();
      visited.add(currentStmt);
      Testkit.logger.error("{}", currentStmt);
      for (Spec spec : currentStmt.transitions.keySet()) {
        Statement next = currentStmt.transitions.get(spec);
        Testkit.logger.error("\t\t{} -> {}", spec, next);
        if (!(pending.contains(next) || visited.contains(next))) {
          pending.add(next);
        }
      }
    }
  }

  boolean isMain() {
    return parent == null;
  }

  private class Statement {
    Set<Statement> children = new HashSet<Statement>();
    final ID id;
    Map<Spec, Statement> transitions = new HashMap<Spec, Statement>();
    Spec spec;

    Statement(Collection<Statement> childStmts) {
      Set<Integer> ids = new HashSet<Integer>();
      for (Statement child : childStmts) {
        for (int cID : child.id.ids) {
          ids.add(cID);
        }
        children.add(child);
      }
      id = new ID(ids);

      globalStmts.put(id, this);
    }

    Statement(int number, Spec spec) {
      id = new ID(number);
      globalStmts.put(id, this);
      this.spec = spec;
      assert number != 6;
    }

/*
    private void setSpec(Spec spec) {
      this.spec = spec;
    }
*/

    private Statement getNextStateOn(Spec spec) {
      return transitions.get(spec);
    }

    @Override
    public String toString() {
/*      StringBuilder sb = new StringBuilder(id.toString());
      sb.append(" [");
      for (Spec spec : transitions.keySet()) {
        sb.append(spec).append(" -> ")
            .append(transitions.get(spec).id).append(" ");
      }
      sb.append("]");
      return sb.toString();*/
      return id.toString();
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Statement && id.equals(((Statement) o).id);
    }

    private void addTransition(Spec spec, Statement next) {
      transitions.put(spec, next);
      input.add(spec);
    }
  }

  private class ID {
    private final Set<Integer> ids;
    ID(Set<Integer> ids) {
      this.ids = ids;
    }

    ID(int id) {
      ids = new HashSet<Integer>(Collections.singletonList(id));
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("{");
      boolean first = true;
      for (int id : ids) {
        if (!first) {
          sb.append(",");
        } else {
          first = false;
        }
        sb.append(id);
      }
      sb.append("}");
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof ID && ids.equals(((ID) o).ids);
    }

    @Override
    public int hashCode() {
      int sum = 0;
      for (int id : ids) sum += id;
      return sum;
    }
  }
}
