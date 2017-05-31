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

import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;

import java.io.Serializable;

public abstract class TMessage implements Msg<TAddress, THeader>, Serializable {

  public final THeader header;

  public TMessage(TAddress src, TAddress dst, Transport protocol) {
    this.header = new THeader(src, dst, protocol);
  }

  @Override
  public THeader getHeader() {
    return this.header;
  }

  @Override
  public TAddress getSource() {
    return this.header.src;
  }

  @Override
  public TAddress getDestination() {
    return this.header.dst;
  }

  @Override
  public Transport getProtocol() {
    return this.header.proto;
  }
}