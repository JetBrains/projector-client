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

import org.w3c.dom.CanvasImageSource
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.ImageBitmapRenderingContext
import org.w3c.dom.RenderingContext

class DomOffscreenCanvas(private val offscreenCanvas: OffscreenCanvas) : Canvas {

  var _context2d : Context2d? = null

  var _bitmapContext: ContextBitmapRenderer? = null

  override fun context2d(): Context2d {
    if( _context2d == null){
      _context2d = DomContext2d(offscreenCanvas.getContext("2d") as OffscreenCanvasRenderingContext2D)
    }
    return _context2d!!
  }

  override fun bitmapContext(): ContextBitmapRenderer {
    TODO("Not yet implemented")
  }

  override var width: Int
    get() = offscreenCanvas.width
    set(value) { offscreenCanvas.width = value }
  override var height: Int
    get() = offscreenCanvas.height
    set(value) { offscreenCanvas.height = value}

  override val imageSource: Canvas.ImageSource
    get() = DomCanvas.DomImageSource(offscreenCanvas)

  override fun takeSnapshot(): Canvas.ImageSource {
    return DomCanvas.DomImageSource(offscreenCanvas)
  }
}
