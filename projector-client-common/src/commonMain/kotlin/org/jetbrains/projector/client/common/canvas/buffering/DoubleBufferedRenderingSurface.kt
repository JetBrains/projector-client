/*
 * MIT License
 *
 * Copyright (c) 2019-2023 JetBrains s.r.o.
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
import org.jetbrains.projector.client.common.canvas.Context2d
import org.jetbrains.projector.client.common.canvas.Context2dStateOperations
import org.jetbrains.projector.client.common.canvas.Extensions.resizeSavingImage

class DoubleBufferedRenderingSurface(bufferCanvasFactory: CanvasFactory, private val target: Canvas) : RenderingSurface {

  override var scalingRatio: Double = 1.0

  private val buffer = Buffer(bufferCanvasFactory, width = target.width, height = target.height)

  override val canvas: Canvas
    get() = buffer

  override fun setBounds(width: Int, height: Int) {
    buffer.resizeSavingImage(width, height)
    target.resizeSavingImage(width, height)
  }

  override fun flush() {
    if (buffer.width == 0 || buffer.height == 0) return

    // optimization: flush only if the buffer is changed
    if (buffer.changed) {
      buffer.resetChanged()
      target.context2d.drawImage(buffer.imageSource, 0.0, 0.0)
    }
  }

  /** A canvas that knows if there have been operations that could modify its bitmap. */
  private class Buffer(bufferCanvasFactory: CanvasFactory, width: Int, height: Int) : Canvas {

    private val delegate = bufferCanvasFactory.create()
      .also {
        it.width = width
        it.height = height
      }

    override val imageSource by delegate::imageSource
    override fun takeSnapshot() = delegate.takeSnapshot()

    var changed = false
      private set

    fun resetChanged() {
      changed = false
    }

    private fun change() {
      changed = true
    }

    private inline fun withChange(operation: Canvas.() -> Unit) {
      change()
      delegate.operation()
    }

    override var width
      get() = delegate.width
      set(value) = withChange { width = value }

    override var height
      get() = delegate.height
      set(value) = withChange { height = value }

    // todo: rewrite boilerplate code to decorators of Context2dDrawOperations when supported: https://youtrack.jetbrains.com/issue/KT-49904
    override val context2d: Context2d = object : Context2dStateOperations by delegate.context2d, Context2d {

      private inline fun withChange2d(operation: Context2d.() -> Unit) {
        change()
        delegate.context2d.operation()
      }

      override fun clearRect(x: Double, y: Double, w: Double, h: Double) = withChange2d { clearRect(x = x, y = y, w = w, h = h) }
      override fun drawImage(imageSource: Canvas.ImageSource, x: Double, y: Double) = withChange2d { drawImage(imageSource, x = x, y = y) }
      override fun drawImage(imageSource: Canvas.ImageSource, x: Double, y: Double, dw: Double, dh: Double) =
        withChange2d { drawImage(imageSource, x = x, y = y, dw = dw, dh = dh) }

      override fun drawImage(
        imageSource: Canvas.ImageSource,
        sx: Double, sy: Double, sw: Double, sh: Double,
        dx: Double, dy: Double, dw: Double, dh: Double,
      ) =
        withChange2d { drawImage(imageSource, sx = sx, sy = sy, sw = sw, sh = sh, dx = dx, dy = dy, dw = dw, dh = dh) }

      override fun stroke() = withChange2d { stroke() }
      override fun fill(fillRule: Context2d.FillRule) = withChange2d { fill(fillRule) }
      override fun fillRect(x: Double, y: Double, w: Double, h: Double) = withChange2d { fillRect(x = x, y = y, w = w, h = h) }
      override fun strokeRect(x: Double, y: Double, w: Double, h: Double) = withChange2d { strokeRect(x = x, y = y, w = w, h = h) }
      override fun strokeText(text: String, x: Double, y: Double) = withChange2d { strokeText(text = text, x = x, y = y) }
      override fun fillText(text: String, x: Double, y: Double) = withChange2d { fillText(text = text, x = x, y = y) }
    }
  }
}
