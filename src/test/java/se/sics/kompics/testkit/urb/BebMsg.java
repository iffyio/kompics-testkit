package se.sics.kompics.testkit.urb;

import se.sics.kompics.network.Transport;
import se.sics.kompics.testkit.fd.TAddress;
import se.sics.kompics.testkit.fd.TMessage;

import java.io.Serializable;

public class BebMsg extends TMessage implements Serializable {

  private static final long serialVersionUID = 94412364060L;

  public final Data data;

  public BebMsg(TAddress src, TAddress dst, Transport proto, Data data) {
    super(src, dst, proto);
    this.data = data;
  }

  public String toString() {
    return String.format("BebMsg (%s, %s, %s)",
            UrbComponent.names.get(super.getSource()),
            UrbComponent.names.get(super.getDestination()), data);
  }
}
