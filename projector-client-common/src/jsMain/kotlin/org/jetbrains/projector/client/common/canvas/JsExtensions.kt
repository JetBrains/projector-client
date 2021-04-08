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

import org.w3c.dom.HTMLCanvasElement


object JsExtensions {

  fun Long.argbIntToRgbaString(): String {
    val argb = this.toInt()
    return argb.argbIntToRgbaString();
  }

  val rgbStrCache = IntRange(0,255)
    .map { i -> if(i < 0x10) "0" + i.toString(16) else i.toString(16)  }
    .toTypedArray()

  /**
   * ARGB -> RGBA
   */
  @Suppress("UNUSED_VARIABLE")
  fun Int.argbIntToRgbaString(): String {
    val argb = this
    val strCache = rgbStrCache
    return js("""
        "#" + strCache[(argb >>> 16) & 0xff] + strCache[(argb >>> 8) & 0xff] + strCache[argb & 0xff] + strCache[(argb >>> 24) & 0xff];
      """).unsafeCast<String>()
  }


}
