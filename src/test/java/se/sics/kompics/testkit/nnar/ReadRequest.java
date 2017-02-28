package se.sics.kompics.testkit.nnar;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.network.Transport;
import se.sics.kompics.testkit.fd.TAddress;
import se.sics.kompics.testkit.fd.TMessage;

import java.io.Serializable;

public class ReadRequest extends TMessage implements KompicsEvent, Serializable{
  public final int rid;

  public ReadRequest(TAddress src, TAddress dst, Transport proto, int rid) {
    super(src, dst, proto);
    this.rid = rid;
  }
}
