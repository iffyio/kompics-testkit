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
package se.sics.kompics.testkit;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;

public abstract class Testkit {

  public static final Logger logger = LoggerFactory.getLogger("Testkit");

  private Testkit() {}

  public static <T extends ComponentDefinition> TestContext<T> newTestContext(
         Class<T> definition, Init<T> initEvent) {
    checkNotNull(definition, initEvent);
    return new TestContext<T>(definition, initEvent);
  }

  public static <T extends ComponentDefinition> TestContext<T> newTestContext(
          Class<T> definition, Init.None initEvent) {
    checkNotNull(definition, initEvent);
    return new TestContext<T>(definition, initEvent);
  }

  static void checkNotNull(Object... objects) {
    for (Object o : objects) {
      Preconditions.checkNotNull(o);
    }
  }

}
