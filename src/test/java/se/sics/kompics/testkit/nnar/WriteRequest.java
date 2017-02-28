package se.sics.kompics.testkit.nnar;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.network.Transport;
import se.sics.kompics.testkit.fd.TAddress;
import se.sics.kompics.testkit.fd.TMessage;

import java.io.Serializable;

public class WriteRequest extends TMessage implements KompicsEvent, Serializable{

  public final int rid;
  public final Register register;

  public WriteRequest(TAddress src, TAddress dst, Transport protocol,
                      int rid, Register register) {
    super(src, dst, protocol);
    this.rid = rid;
    this.register = register;
  }
}
