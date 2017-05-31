package se.sics.kompics.testkit.fd;

import se.sics.kompics.network.Transport;

import java.io.Serializable;

public class Pong extends TMessage implements Serializable {

  private static final long serialVersionUID = -647229566141L;

  public Pong(TAddress src, TAddress dst) {
    super(src, dst, Transport.TCP);
  }

  public boolean equals(Object o) {
    if (!(o instanceof Pong)) {
      return false;
    }

    Pong other = (Pong) o;
    return getSource().equals(other.getSource()) &&
            getDestination().equals(other.getDestination()) &&
            getProtocol().equals(other.getProtocol());
  }
}
