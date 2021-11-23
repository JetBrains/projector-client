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
package org.jetbrains.projector.client.common.canvas

interface CanvasFactory {
  fun create(): Canvas
  fun createImageSource(pngBase64: String, onLoad: (Canvas.ImageSource) -> Unit)
  fun createEmptyImageSource(onLoad: (Canvas.ImageSource) -> Unit)
}

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
