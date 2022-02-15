/*
 * MIT License
 *
 * Copyright (c) 2019-2022 JetBrains s.r.o.
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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.openqa.selenium.Keys

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

class ToSeleniumKeysTest : FunSpec() {
  init {
    test("simple symbol should be typed") {
      toSeleniumKeys("h").toList() shouldBe "h".toList()
    }

    test("simple symbol should be typed with Shift modifier") {
      toSeleniumKeys("h", shift = true).toList() shouldBe Keys.chord(Keys.SHIFT, "h").toList()
    }

    test("already Shifted simple symbol should be typed") {
      toSeleniumKeys("H").toList() shouldBe "H".toList()
    }

    test("Tab should be typed") {
      toSeleniumKeys(Keys.TAB).toList() shouldBe Keys.TAB.toList()
    }

    test("Enter should be typed") {
      toSeleniumKeys(Keys.ENTER).toList() shouldBe Keys.ENTER.toList()
    }

    test("Backspace should be typed") {
      toSeleniumKeys(Keys.BACK_SPACE).toList() shouldBe Keys.BACK_SPACE.toList()
    }

    test("Space should be typed") {
      toSeleniumKeys(Keys.SPACE).toList() shouldBe Keys.SPACE.toList()
    }

    test("Escape should be typed") {
      toSeleniumKeys(esc = true).toList() shouldBe Keys.ESCAPE.toList()
    }

    test("Delete should be typed") {
      toSeleniumKeys(Keys.DELETE).toList() shouldBe Keys.DELETE.toList()
    }

    test("letter should be typed with Ctrl modifier") {
      toSeleniumKeys("z", ctrl = true).toList() shouldBe Keys.chord(Keys.CONTROL, "z").toList()
    }

    test("functional key should be typed") {
      toSeleniumKeys(f = Keys.F6).toList() shouldBe Keys.F6.toList()
    }

    test("functional key with Shift modifier should be typed") {
      toSeleniumKeys(f = Keys.F6, shift = true).toList() shouldBe Keys.chord(Keys.SHIFT, Keys.F6).toList()
    }

    test("functional key with Shift and Ctrl modifier should be typed") {
      toSeleniumKeys("k", ctrl = true, shift = true).toList() shouldBe Keys.chord(Keys.CONTROL, Keys.SHIFT, "k").toList()
    }

    test("arrow should be typed") {
      toSeleniumKeys(Keys.ARROW_RIGHT).toList() shouldBe Keys.ARROW_RIGHT.toList()
    }

    test("numpad key should be typed with numlock") {
      toSeleniumKeys(Keys.NUMPAD5).toList() shouldBe Keys.NUMPAD5.toList()
    }

    test("numpad key should be typed without numlock") {
      toSeleniumKeys("\uE057").toList() shouldBe "\uE057".toList()
    }
  }
}
