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

import org.jetbrains.projector.client.common.canvas.Canvas.ImageSource
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.*

class DomCanvas(private val myCanvas: HTMLCanvasElement) : Canvas {

  var _context2d : Context2d? = null

  var _bitmapContext: ContextBitmapRenderer? = null

  override fun context2d(): Context2d {
    if( _context2d == null){
      _context2d = DomContext2d(myCanvas.getContext("2d",js("{ desynchronized: true }")) as CanvasRenderingContext2D)
    }
    return _context2d!!
  }

  override fun bitmapContext(): ContextBitmapRenderer {
    if( _bitmapContext == null){
      _bitmapContext = DomContextBitmapRenderer(myCanvas.getContext("bitmaprenderer") as ImageBitmapRenderingContext)
    }
    return _bitmapContext!!
  }

  override var width: Int
    get() = myCanvas.width
    set(value) {
      myCanvas.width = value
    }

  override var height: Int
    get() = myCanvas.height
    set(value) {
      myCanvas.height = value
    }

  override var fontVariantLigatures: String
    get() = myCanvas.style.fontVariantLigatures
    set(value) {
      myCanvas.style.fontVariantLigatures = value
    }
  override val imageSource: ImageSource = DomImageSource(myCanvas)

  override fun takeSnapshot(): ImageSource {
    //val copy = document.createElement("canvas") as HTMLCanvasElement
    //
    //copy.apply {
    //  width = myCanvas.width
    //  height = myCanvas.height
    //  if (myCanvas.width != 0 && myCanvas.height != 0) {
    //    (getContext("2d") as CanvasRenderingContext2D).drawImage(myCanvas, 0.0, 0.0)
    //  }
    //}

    val copy = OffscreenCanvas(myCanvas.width,myCanvas.height)

    val context = copy.getContext("2d",js("{ alpha: false }")) as OffscreenCanvasRenderingContext2D

    context.drawImage(myCanvas,0.0,0.0)

    return DomImageSource(copy)
  }

  open class DomImageSource(val canvasElement: CanvasImageSource) : ImageSource {
    override fun isEmpty(): Boolean {
      return canvasElement.isEmpty()
    }
  }

  companion object {

    private val logger = Logger<DomCanvas>()

    fun CanvasImageSource.isEmpty(): Boolean =
      when (this) {
        is HTMLImageElement -> width == 0 || height == 0
        is HTMLCanvasElement -> width == 0 || height == 0
        is HTMLVideoElement -> width == 0 || height == 0
        is ImageBitmap -> width == 0 || height == 0
        is ImageData -> width == 0 || height == 0
        is OffscreenCanvas -> width == 0 || height == 0
        else -> {
          logger.error { "Unknown type ${this::class}" }
          false
        }
      }
  }
}
