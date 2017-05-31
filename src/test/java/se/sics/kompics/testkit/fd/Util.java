package se.sics.kompics.testkit.fd;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Util {

  public static TAddress createAddress(int port) {
    try {
      InetAddress ip = InetAddress.getByName("127.0.0.1");
      return new TAddress(ip, port);
    } catch (UnknownHostException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static TAddress getPongerAddr() {
    return createAddress(34567);
  }

  public static TAddress getEPFDAddr() {
    return createAddress(45678);
  }
}
