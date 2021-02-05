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
package org.jetbrains.projector.client.common

import org.jetbrains.projector.util.logging.Logger
import java.awt.Font
import java.io.ByteArrayInputStream

object SwingFontCache {
  private val logger = Logger<SwingFontCache>()

  private val knownFontsByName = HashMap<String, Font>()
  private val knownFonts = HashMap<String, Font>()

  fun registerServerFont(index: Int, data: ByteArray) {
    knownFontsByName["serverFont$index"] = Font.createFont(Font.TRUETYPE_FONT, ByteArrayInputStream(data))
  }

  fun getFont(name: String): Font {
    // font names are "12px Arial"
    // or "12px serverFont0"

    return knownFonts.getOrPut(name) {
      logger.info { "Loading new font for $name" }
      val split = name.split(' ', limit = 2)
      knownFontsByName.getOrPut(split[1]) {
        logger.info { "Loading new font type ${split[1]}" }
        Font(split[1], 0, 12)
      }.deriveFont(split[0].removeSuffix("px").toFloat())
    }
  }
}