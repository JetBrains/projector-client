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

/**
 *   class JvmCanvas(private val myPanel: JPanel) : Canvas {
 *     override val context2d: Context2d = JvmContext2d(myPanel.graphics)
 *   }
 *
 *   class JvmContext2d(private val myGraphics: Graphics) : Context2d {
 *     override fun clearRect(x: Double, y: Double, w: Double, h: Double) {
 *       myGraphics.clearRect(x, y, w, h)
 *     }
 *   }
 *
 *   class AndroidCanvas(private val nativeCanvas: Canvas) : Canvas {
 *     override val context2d: Context2d = AndroidContext2d(nativeCanvas)
 *   }
 *
 *   class AndroidContext2d(private val myNativeCanvas: Canvas) : Context2d {
 *     override fun clearRect(x: Double, y: Double, w: Double, h: Double) {
 *       myNativeCanvas.clipRect(x, y, w, h)
 *       myNativeCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
 *     }
 *   }
 */

interface Canvas {
  val context2d: Context2d
  var width: Int
  var height: Int
  var fontVariantLigatures: String
    get() = ""
    set(value) {}
  val imageSource: ImageSource

  fun takeSnapshot(): Snapshot

  interface ImageSource {
    fun isEmpty(): Boolean
  }

  // Unchangeable copy of canvas data
  interface Snapshot : ImageSource
}
