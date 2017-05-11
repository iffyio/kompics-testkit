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
}
