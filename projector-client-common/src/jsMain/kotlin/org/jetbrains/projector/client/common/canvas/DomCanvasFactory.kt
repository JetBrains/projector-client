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

import kotlinx.browser.document
import org.w3c.dom.CanvasImageSource
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image

object DomCanvasFactory : CanvasFactory {
  override fun create(): Canvas {
    return DomCanvas(document.createElement("canvas") as HTMLCanvasElement)
  }

  override fun createImageSource(pngBase64: String, onLoad: (Canvas.ImageSource) -> Unit) {
    Image().apply {
      src = "data:image/png;base64,${pngBase64}"
      onload = { onLoad(DomCanvas.DomImageSource(this)) }
    }
  }

  override fun createEmptyImageSource(onLoad: (Canvas.ImageSource) -> Unit) {
    createEmptyImage()
      .run(DomCanvas::DomImageSource)
      .run(onLoad)
  }

  private fun createEmptyImage(): CanvasImageSource = (document.createElement("div") as HTMLCanvasElement).apply {
    width = 20
    height = 20

    (getContext("2d") as CanvasRenderingContext2D).apply {
      fillStyle = "pink"
      fillRect(0.0, 0.0, 20.0, 20.0)
    }
  }
}
