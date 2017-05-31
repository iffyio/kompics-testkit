/**
 * This file is part of the Kompics Testing runtime.
 *
 * Copyright (C) 2017 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2017 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.testing.fd;

import se.sics.kompics.network.Address;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;



public class TAddress implements Address, Serializable {

  private final InetSocketAddress isa;
  public int group, rank;

  public TAddress(InetAddress addr, int port) {
    this.isa = new InetSocketAddress(addr, port);
  }

  @Override
  public InetAddress getIp() {
    return this.isa.getAddress();
  }

  @Override
  public int getPort() {
    return this.isa.getPort();
  }

  @Override
  public InetSocketAddress asSocket() {
    return this.isa;
  }

  @Override
  public boolean sameHostAs(Address other) {
    return this.isa.equals(other.asSocket());
  }

  @Override
  public final String toString() {
    //return "<" + isa.getAddress().toString().substring(11) + ">";
    return isa.toString();
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 11 * hash + this.isa.hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final TAddress other = (TAddress) obj;
    if (this.isa != other.isa && (this.isa == null || !this.isa.equals(other.isa)))
      return false;
    return true;
  }
}
