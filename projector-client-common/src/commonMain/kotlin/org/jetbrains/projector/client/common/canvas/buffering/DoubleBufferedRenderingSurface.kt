/*
 * MIT License
 *
 * Copyright (c) 2019-2021 JetBrains s.r.o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.jetbrains.projector.client.common.canvas.buffering

import org.jetbrains.projector.client.common.canvas.Canvas
import org.jetbrains.projector.client.common.canvas.CanvasFactory
import org.jetbrains.projector.client.common.canvas.Extensions.resizeSavingImage

class DoubleBufferedRenderingSurface(private val canvasFactory: CanvasFactory, private val target: Canvas) : RenderingSurface {

  override var scalingRatio: Double = 1.0

  private val buffer = createBuffer()

  override val canvas: Canvas
    get() = buffer

  override fun setBounds(width: Int, height: Int) {
    buffer.resizeSavingImage(width, height)
    target.resizeSavingImage(width, height)
  }

  override fun flush() {
    if (buffer.width == 0 || buffer.height == 0) return

    target.context2d.drawImage(buffer.imageSource, 0.0, 0.0)
  }

  private fun createBuffer(): Canvas {
    return canvasFactory.create()
      .apply {
        width = target.width
        height = target.height
      }
  }
}
