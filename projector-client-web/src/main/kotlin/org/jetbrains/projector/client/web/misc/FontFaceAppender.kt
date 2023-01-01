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
package org.jetbrains.projector.client.web.misc

import kotlinx.browser.document
import org.jetbrains.projector.client.common.canvas.Extensions.toFontFaceName
import org.jetbrains.projector.common.protocol.data.TtfFontData
import kotlin.js.Promise

private external class FontFace(family: String, src: String) {  // todo: remove after https://youtrack.jetbrains.com/issue/KT-34124

  fun load(): Promise<FontFace>
}

object FontFaceAppender {

  private var loadedFonts = 0

  fun appendFontFaceToPage(fontId: Short, fontData: TtfFontData, onLoad: (Int) -> Unit) {
    val fontFaceName = fontId.toFontFaceName()

    // todo: try to use the ByteArray variant:
    val fontFace = FontFace(fontFaceName, "url(data:font/truetype;base64,${fontData.ttfBase64}) format('truetype')")

    fontFace.load().then {
      document.asDynamic().fonts.add(it)  // todo: remove asDynamic() after https://youtrack.jetbrains.com/issue/KT-34124

      ++loadedFonts
      onLoad(loadedFonts)
    }
  }

  fun removeAppendedFonts() {
    document.asDynamic().fonts.clear()  // todo: remove asDynamic() after https://youtrack.jetbrains.com/issue/KT-34124
    loadedFonts = 0
  }
}
