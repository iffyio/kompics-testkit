package se.sics.kompics.testkit.scheduler;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.Scheduler;

public class CallingThreadScheduler extends Scheduler{

  @Override
  public void schedule(Component c, int wid) {
    ((ComponentCore) c).execute(wid);
  }

  @Override
  public void proceed() { }

  @Override
  public void shutdown() { }

  @Override
  public void asyncShutdown() { }
}
