package se.sics.kompics.testkit.urb;

import se.sics.kompics.testkit.fd.TAddress;

import java.io.Serializable;

public class Data implements Serializable{

  private static final long serialVersionUID = 1339912364060L;

  public TAddress sender;
  public Serializable msg;

  public Data(TAddress sender, Serializable msg) {
    this.sender = sender;
    this.msg = msg;
  }

  public String toString() {
    return String.format("Data(%s)", msg);
  }

  public boolean equals(Object o) {
    return o instanceof Data && msg.equals(((Data) o).msg);
  }

  public int hashCode() {
    return msg.hashCode();
  }
}
