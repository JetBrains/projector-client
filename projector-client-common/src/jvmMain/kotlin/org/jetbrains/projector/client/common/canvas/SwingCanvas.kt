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

import java.awt.Image
import java.awt.image.BufferedImage
import java.lang.Math.min

class SwingCanvas() : Canvas {

  var image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    private set

  var _context2d: Context2d = SwingContext2d(image.createGraphics())

  override fun context2d(): Context2d  = _context2d
  override fun bitmapContext(): ContextBitmapRenderer {
    TODO("Not yet implemented")
  }


  override var width: Int
    get() = image.width
    set(value) { doResize(value, height) }
  override var height: Int
    get() = image.height
    set(value) { doResize(width, value) }

  private fun doResize(width: Int, height: Int) {
    val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    newImage.createGraphics().drawImage(image, 0, 0, min(width, image.width), min(height, image.height), null)
    image = newImage
    _context2d = SwingContext2d(image.createGraphics())
  }

  override val imageSource: Canvas.ImageSource
    get() = SwingImageSource(image)

  override fun takeSnapshot(): Canvas.ImageSource {
    val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    newImage.createGraphics().drawImage(image, 0, 0, null)
    return SwingImageSource(newImage)
  }

  open class SwingImageSource(val image: Image) : Canvas.ImageSource {
    constructor() : this(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))

    override fun isEmpty(): Boolean {
      return image.getWidth(null) == 1 && image.getHeight(null) == 1
    }
  }
}
