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
package org.jetbrains.projector.client.web.speculative

import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier
import kotlin.test.Test
import kotlin.test.assertEquals

class TypingTest {

  private val typing = Typing.SpeculativeTyping { null }

  @Suppress("TestFunctionName")
  private fun KeyPressed(char: Char, vararg modifiers: KeyModifier) = ClientKeyPressEvent(0, char, setOf(*modifiers))

  @Test
  fun testSpeculativeTypingFiltering() {

    listOf(
      KeyPressed('a')                               /* Just a regular char */                 to false, // Category: LOWERCASE_LETTER
      KeyPressed('B',       KeyModifier.CTRL_KEY)                                             to true,  // Category: UPPERCASE_LETTER
      KeyPressed('1',       KeyModifier.ALT_KEY)                                              to true,  // Category: DECIMAL_DIGIT_NUMBER
      KeyPressed('(',       KeyModifier.META_KEY)                                             to true,  // Category: START_PUNCTUATION
      KeyPressed(')',       KeyModifier.REPEAT)                                               to false, // Category: END_PUNCTUATION
      KeyPressed(',',       KeyModifier.SHIFT_KEY)                                            to false, // Category: OTHER_PUNCTUATION
      KeyPressed('+',       KeyModifier.CTRL_KEY, KeyModifier.META_KEY)                       to true,  // Category: MATH_SYMBOL
      KeyPressed('-',       KeyModifier.CTRL_KEY, KeyModifier.ALT_KEY, KeyModifier.META_KEY)  to true,  // Category: DASH_PUNCTUATION
      KeyPressed('`',       KeyModifier.CTRL_KEY, KeyModifier.SHIFT_KEY)                      to true,  // Category: MODIFIER_SYMBOL
      KeyPressed(' ',       KeyModifier.REPEAT, KeyModifier.SHIFT_KEY)                        to false, // Category: SPACE_SEPARATOR
      KeyPressed('\u0000')                          /* <Null> */                              to true,  // Category: CONTROL
      KeyPressed('\u001B',  KeyModifier.CTRL_KEY)   /* <Escape> */                            to true,  // Category: CONTROL
      KeyPressed('\n',      KeyModifier.META_KEY, KeyModifier.ALT_KEY)                        to true,  // Category: CONTROL
      KeyPressed('\r',      KeyModifier.SHIFT_KEY)                                            to true,  // Category: CONTROL
      KeyPressed('\uFEFF',  KeyModifier.REPEAT)     /* BOM */                                 to true,  // Category: FORMAT
      KeyPressed('\uFFFF')                          /* Undefined Character */                 to true,  // Category: UNASSIGNED
    ).forEachIndexed { index, (event, expected) ->

      val result = typing.shouldSkipEvent(event)
      assertEquals(expected, result, "Differs at index $index for event $event")
    }

  }

}
