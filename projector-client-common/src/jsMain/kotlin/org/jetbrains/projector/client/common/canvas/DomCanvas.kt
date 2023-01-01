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
package org.jetbrains.projector.client.common.canvas

import kotlinx.browser.document
import org.jetbrains.projector.client.common.canvas.Canvas.ImageSource
import org.jetbrains.projector.client.common.canvas.Canvas.Snapshot
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.*

class DomCanvas(private val myCanvas: HTMLCanvasElement) : Canvas {

  override val context2d: Context2d = DomContext2d(myCanvas.getContext("2d") as CanvasRenderingContext2D)

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

  override fun takeSnapshot(): Snapshot {
    val copy = document.createElement("canvas") as HTMLCanvasElement

    copy.apply {
      width = myCanvas.width
      height = myCanvas.height
      if (myCanvas.width != 0 && myCanvas.height != 0) {
        (getContext("2d") as CanvasRenderingContext2D).drawImage(myCanvas, 0.0, 0.0)
      }
    }

    return object : DomImageSource(copy), Snapshot {}
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
        else -> {
          logger.error { "Unknown type ${this::class}" }
          false
        }
      }
  }
}
