package se.sics.kompics.testkit.fd;

import se.sics.kompics.PortType;

public class EPFDPort extends PortType{

  {
    request(Watch.class);
    indication(Restore.class);
    indication(Suspect.class);
  }

}