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
package org.jetbrains.projector.intTest.headless.keyboard

import com.codeborne.selenide.Selenide.element
import org.openqa.selenium.Keys
import kotlin.test.Test
import kotlin.test.assertEquals

fun inputOnDesktop(vararg keysToSend: CharSequence, ctrl: Boolean, shift: Boolean, f: Keys?, esc: Boolean) {
  val keys = toSeleniumKeys(*keysToSend, ctrl = ctrl, shift = shift, f = f, esc = esc)
  element("body").sendKeys(keys)
}

private fun toSeleniumKeys(
  vararg keysToSend: CharSequence,
  ctrl: Boolean = false,
  shift: Boolean = false,
  f: Keys? = null,
  esc: Boolean = false,
): CharSequence {
  var keys: CharSequence = keysToSend.joinToString("")

  if (f != null) {
    keys = "$keys$f"
  }
  if (esc) {
    keys = "$keys${Keys.ESCAPE}"
  }
  when {
    ctrl && shift -> keys = Keys.chord(Keys.CONTROL, Keys.SHIFT, keys)
    shift -> keys = Keys.chord(Keys.SHIFT, keys)
    ctrl -> keys = Keys.chord(Keys.CONTROL, keys)
  }
  return keys
}

class ToSeleniumKeysTest {

  @Test
  fun testSimpleSymbol() = assertEquals(
    expected = "h".toList(),
    actual = toSeleniumKeys("h").toList(),
  )

  @Test
  fun testShiftedSimpleSymbol() = assertEquals(
    expected = Keys.chord(Keys.SHIFT, "h").toList(),
    actual = toSeleniumKeys("h", shift = true).toList(),
  )

  @Test
  fun testAlreadyShiftedSimpleSymbol() = assertEquals(
    expected = "H".toList(),
    actual = toSeleniumKeys("H").toList(),
  )

  @Test
  fun testTab() = assertEquals(
    expected = Keys.TAB.toList(),
    actual = toSeleniumKeys(Keys.TAB).toList(),
  )

  @Test
  fun testEnter() = assertEquals(
    expected = Keys.ENTER.toList(),
    actual = toSeleniumKeys(Keys.ENTER).toList(),
  )

  @Test
  fun testBackspace() = assertEquals(
    expected = Keys.BACK_SPACE.toList(),
    actual = toSeleniumKeys(Keys.BACK_SPACE).toList(),
  )

  @Test
  fun testSpace() = assertEquals(
    expected = Keys.SPACE.toList(),
    actual = toSeleniumKeys(Keys.SPACE).toList(),
  )

  @Test
  fun testEscape() = assertEquals(
    expected = Keys.ESCAPE.toList(),
    actual = toSeleniumKeys(esc = true).toList(),
  )

  @Test
  fun testDelete() = assertEquals(
    expected = Keys.DELETE.toList(),
    actual = toSeleniumKeys(Keys.DELETE).toList(),
  )

  @Test
  fun testCtrlLetter() = assertEquals(
    expected = Keys.chord(Keys.CONTROL, "z").toList(),
    actual = toSeleniumKeys("z", ctrl = true).toList(),
  )

  @Test
  fun testFunctionalKey() = assertEquals(
    expected = Keys.F6.toList(),
    actual = toSeleniumKeys(f = Keys.F6).toList(),
  )

  @Test
  fun testShiftedFunctionalKey() = assertEquals(
    expected = Keys.chord(Keys.SHIFT, Keys.F6).toList(),
    actual = toSeleniumKeys(f = Keys.F6, shift = true).toList(),
  )

  @Test
  fun testCtrlShiftedLetter() = assertEquals(
    expected = Keys.chord(Keys.CONTROL, Keys.SHIFT, "k").toList(),
    actual = toSeleniumKeys("k", ctrl = true, shift = true).toList(),
  )

  @Test
  fun testArrow() = assertEquals(
    expected = Keys.ARROW_RIGHT.toList(),
    actual = toSeleniumKeys(Keys.ARROW_RIGHT).toList(),
  )

  @Test
  fun testNumpadWithNumLock() = assertEquals(
    expected = Keys.NUMPAD5.toList(),
    actual = toSeleniumKeys(Keys.NUMPAD5).toList(),
  )

  @Test
  fun testNumpadWithoutNumLock() = assertEquals(
    expected = "\uE057".toList(),
    actual = toSeleniumKeys("\uE057").toList(),
  )
}
