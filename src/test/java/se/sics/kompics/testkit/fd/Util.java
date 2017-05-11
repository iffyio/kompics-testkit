/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
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
