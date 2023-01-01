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
package org.jetbrains.projector.client.web.state

import org.jetbrains.projector.client.common.canvas.Extensions.argbIntToRgbaString
import org.jetbrains.projector.common.protocol.toClient.ServerWindowColorsEvent

interface LafListener {
  fun lookAndFeelChanged()
}

// Name of this class was chosen based on JBUI class.
object ProjectorUI {
  var windowHeaderActiveBackgroundArgb = 0xFFE6E6E6.toInt()
    private set

  var windowHeaderInactiveBackgroundArgb = 0xFFEDEDED.toInt()
    private set

  var windowActiveBorderArgb = 0xFFD5D5D5.toInt()
    private set

  var windowInactiveBorderArgb = 0xFFAAAAAA.toInt()
    private set

  var windowHeaderActiveTextArgb = 0xFF1A1A1A.toInt()
    private set

  var windowHeaderInactiveTextArgb = 0xFFDDDDDD.toInt()
    private set

  var borderStyle = "1px solid ${windowActiveBorderArgb.argbIntToRgbaString()}"
    private set

  const val crossOffset = 4.0
  const val headerHeight = 28.0
  const val borderRadius = 8
  const val borderThickness = 8.0

  fun setColors(colors: ServerWindowColorsEvent.ColorsStorage) {
    windowHeaderActiveBackgroundArgb = colors.windowHeaderActiveBackground.argb
    windowHeaderInactiveBackgroundArgb = colors.windowHeaderInactiveBackground.argb
    windowActiveBorderArgb = colors.windowActiveBorder.argb
    borderStyle = "1px solid ${windowActiveBorderArgb.argbIntToRgbaString()}"
    windowInactiveBorderArgb = colors.windowInactiveBorder.argb
    windowHeaderActiveTextArgb = colors.windowHeaderActiveText.argb
    windowHeaderInactiveTextArgb = colors.windowHeaderInactiveText.argb
  }
}
