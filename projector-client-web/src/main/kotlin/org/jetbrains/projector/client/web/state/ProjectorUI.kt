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
package org.jetbrains.projector.client.web.state

import org.jetbrains.projector.common.protocol.data.PaintValue

interface LafListener {
  fun lookAndFeelChanged()
}

// Name of this class was chosen based on JBUI class.
object ProjectorUI {
  var windowHeaderActiveBackground = "#E6E6E6"
    private set

  var windowHeaderInactiveBackground = "#EDEDED"
    private set

  var windowActiveBorder = "#D5D5D5"
    private set

  var windowInactiveBorder = "#AAAAAA"
    private set

  var windowHeaderActiveText = "#1A1A1A"
    private set

  var windowHeaderInactiveText = "#DDDDDD"
    private set

  var borderStyle = "1px solid $windowActiveBorder"
    private set

  const val crossOffset = 4.0
  const val headerHeight = 28.0
  const val borderRadius = 8
  const val borderThickness = 8.0

  private fun rgbColorFromInt(color: Int) = "rgb(${color shr 16 and 0xFF}, ${color shr 8 and 0xFF}, ${color and 0xFF})"

  fun setColors(colors: Map<String, PaintValue.Color>) {
    colors.forEach {
      when (it.key) {
        "windowHeaderActiveBackground" -> windowHeaderActiveBackground = rgbColorFromInt(it.value.argb)
        "windowHeaderInactiveBackground" -> windowHeaderInactiveBackground = rgbColorFromInt(it.value.argb)
        "windowActiveBorder" -> {
          windowActiveBorder = rgbColorFromInt(it.value.argb)
          borderStyle = "1px solid $windowActiveBorder"
        }
        "windowInactiveBorder" -> windowInactiveBorder = rgbColorFromInt(it.value.argb)
        "windowHeaderActiveText" -> windowHeaderActiveText = rgbColorFromInt(it.value.argb)
        "windowHeaderInactiveText" -> windowHeaderInactiveText = rgbColorFromInt(it.value.argb)
      }
    }
  }
}
