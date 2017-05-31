package se.sics.kompics.testkit.fd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;

public class Watcher extends ComponentDefinition{
  private static final Logger logger = LoggerFactory.getLogger(Ponger.class);
  Positive<EPFDPort> epfdPort = requires(EPFDPort.class);
  TAddress epfdAddr, pongerAddr;
  public Watcher() {

    init();
    epfdAddr = Util.getEPFDAddr();
    pongerAddr = Util.getPongerAddr();

    Component timer = create(JavaTimer.class, Init.NONE);
    Component networkEPFD = create(NettyNetwork.class, new NettyInit(epfdAddr));
    Component networkPonger = create(NettyNetwork.class, new NettyInit(pongerAddr));
    Component ponger = create(Ponger.class, Init.NONE);
    Component epfd = create(EPFD.class, Init.NONE);


    connect(epfd.getNegative(Timer.class), timer.getPositive(Timer.class));
    connect(epfd.getNegative(Network.class), networkEPFD.getPositive(Network.class));

    connect(epfdPort.getPair(), epfd.getPositive(EPFDPort.class));
    connect(ponger.getNegative(Network.class), networkPonger.getPositive(Network.class));

  }

  private Handler<Start> startHandler = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      logger.info("watcher started! epfd at {}", epfdAddr);
      Watch w = new Watch(pongerAddr);
      trigger(w, epfdPort);
    }
  };

  private Handler<Restore> restoreHandler = new Handler<Restore>() {
    @Override
    public void handle(Restore event) {
      logger.info("restoring {}", event.node);
    }
  };
  private Handler<Suspect> suspectHandler = new Handler<Suspect>() {
    @Override
    public void handle(Suspect event) {
      logger.info("suspecting {}", event.node);
    }
  };

  private void init() {
    //self = createAddr("pingpong.fd.host", "pingpong.fd.port");
    subscribe(startHandler, control);
    subscribe(suspectHandler, epfdPort);
    subscribe(restoreHandler, epfdPort);
  }

}
