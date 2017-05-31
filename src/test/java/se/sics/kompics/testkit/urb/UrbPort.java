package se.sics.kompics.testkit.urb;

import se.sics.kompics.PortType;

public class UrbPort extends PortType{
  {
    request(UrbBroadcast.class);
    indication(UrbDeliver.class);
  }
}
