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
package org.jetbrains.projector.client.common.misc

import org.jetbrains.projector.client.common.canvas.Canvas
import org.jetbrains.projector.client.common.canvas.CanvasFactory
import org.jetbrains.projector.client.common.canvas.Context2d

object DummyCanvasFactory : CanvasFactory {
  private class DummyCanvas: Canvas {
    override val context2d: Context2d
      get() = error("DummyCanvas has no context2d")
    override var width: Int = 1
    override var height: Int = 1
    override val imageSource = DummyImageSource()
    override fun takeSnapshot() = DummyImageSource()
  }

  private class DummyImageSource : Canvas.Snapshot {
    override fun isEmpty() = true
  }

  override fun create(): Canvas = DummyCanvas()
  override fun createImageSource(pngBase64: String, onLoad: (Canvas.ImageSource) -> Unit) = onLoad(DummyImageSource())
  override fun createEmptyImageSource(onLoad: (Canvas.ImageSource) -> Unit) = onLoad(DummyImageSource())
}
