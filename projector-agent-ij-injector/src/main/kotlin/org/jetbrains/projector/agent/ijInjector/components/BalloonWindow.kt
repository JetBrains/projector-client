/*
 * Copyright (c) 2019-2021, JetBrains s.r.o. and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. JetBrains designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact JetBrains, Na Hrebenech II 1718/10, Prague, 14000, Czech Republic
 * if you need additional information or have any questions.
 */
package org.jetbrains.projector.agent.ijInjector.components

import org.jetbrains.projector.util.logging.Logger
import java.awt.Graphics
import java.awt.Window
import javax.swing.JWindow

public open class BalloonWindow @JvmOverloads constructor(owner: Window? = null, public val isButton: Boolean = false) : JWindow(owner) {

  override fun paint(g: Graphics?) {
    logger.debug { "paint" }
    super.paint(g)
  }

  override fun paintAll(g: Graphics?) {
    logger.debug { "paintAll" }
    super.paintAll(g)
  }

  private companion object {

    val logger = Logger<BalloonWindow>()

  }
}
