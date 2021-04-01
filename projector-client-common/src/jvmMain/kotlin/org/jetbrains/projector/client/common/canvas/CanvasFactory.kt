/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO

actual object CanvasFactory {
  actual fun create(): Canvas {
    return SwingCanvas()
  }

  actual fun create(offscreen: Boolean): Canvas {
    TODO("Not yet implemented")
  }

  actual fun createImageSource(
    pngBase64: String,
    onLoad: (Canvas.ImageSource) -> Unit,
  ) {
    val image = ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(pngBase64)))
    onLoad(SwingCanvas.SwingImageSource(image))
  }

  actual fun createEmptyImageSource(onLoad: (Canvas.ImageSource) -> Unit) {
    onLoad(SwingCanvas.SwingImageSource())
  }
}
