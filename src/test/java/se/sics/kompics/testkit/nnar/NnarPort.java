package se.sics.kompics.testkit.nnar;

import se.sics.kompics.PortType;

public class NnarPort extends PortType{

  {
    request(Read.class);
    request(Write.class);
    indication(ReadReturn.class);
    indication(WriteReturn.class);
  }
}
