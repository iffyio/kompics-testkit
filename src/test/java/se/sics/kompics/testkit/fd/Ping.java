package se.sics.kompics.testkit.fd;

import se.sics.kompics.network.Transport;

import java.io.Serializable;

public class Ping extends TMessage implements Serializable {

  private static final long serialVersionUID = 647229141L;

  public Ping(TAddress src, TAddress dst) {
    super(src, dst, Transport.TCP);
  }

  public boolean equals(Object o) {
    if (!(o instanceof Ping)) {
      return false;
    }

    Ping other = (Ping) o;
    return getSource().equals(other.getSource()) &&
           getDestination().equals(other.getDestination()) &&
           getProtocol().equals(other.getProtocol());
  }

  @Override
  public int hashCode() {
    int result = 31 * getSource().hashCode();
    result += 31 * getDestination().hashCode();
    result += 31 * getProtocol().hashCode();
    return result;
  }
}