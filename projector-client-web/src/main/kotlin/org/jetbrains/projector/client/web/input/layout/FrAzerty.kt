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
package org.jetbrains.projector.client.web.input.layout

import org.jetbrains.projector.common.protocol.data.VK

object FrAzerty : KeyboardLayout {

  private val frAzertyCodeMap = mapOf(
    // number row:
    "Backquote" to VK.UNDEFINED,  // '²'
    "Digit1" to VK.AMPERSAND,  // '&'
    "Digit2" to VK.UNDEFINED,  // 'é'
    "Digit3" to VK.QUOTEDBL,  // '"'
    "Digit4" to VK.QUOTE,  // '''
    "Digit5" to VK.LEFT_PARENTHESIS,  // '('
    "Digit6" to VK.MINUS,  // '-'
    "Digit7" to VK.UNDEFINED,  // 'è'
    "Digit8" to VK.UNDERSCORE,  // '_'
    "Digit9" to VK.UNDEFINED,  // 'ç'
    "Digit0" to VK.UNDEFINED,  // 'à'
    "Minus" to VK.RIGHT_PARENTHESIS,  // ')'
    // 1st letter row
    "KeyQ" to VK.A,
    "KeyW" to VK.Z,
    "BracketLeft" to VK.DEAD_CIRCUMFLEX,  // '^'
    "BracketRight" to VK.DOLLAR,  // '$'
    "Backslash" to VK.ASTERISK,  // '*'
    // 2nd letter row
    "KeyA" to VK.Q,
    "Semicolon" to VK.M,
    "Quote" to VK.UNDEFINED,  // 'ù'
    // 3rd letter row
    "KeyZ" to VK.W,
    "KeyM" to VK.COMMA,
    "Comma" to VK.SEMICOLON,
    "Period" to VK.COLON,
    "Slash" to VK.EXCLAMATION_MARK,
  )

  override fun getVirtualKey(code: String): VK? = frAzertyCodeMap[code] ?: UsQwerty.getVirtualKey(code)
}
