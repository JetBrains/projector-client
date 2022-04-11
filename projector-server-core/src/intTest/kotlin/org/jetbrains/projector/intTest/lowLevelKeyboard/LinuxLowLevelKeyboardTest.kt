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
package org.jetbrains.projector.intTest.lowLevelKeyboard

import com.codeborne.selenide.Configuration
import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.Spec
import org.junit.jupiter.api.condition.OS
import java.awt.event.KeyEvent
import kotlin.reflect.KClass

class LinuxOnlyCondition : EnabledCondition {
  override fun enabled(kclass: KClass<out Spec>): Boolean = when {
    kclass.simpleName?.contains("Linux") == true -> OS.LINUX.isCurrentOs
    else -> true // non Linux tests always run
  }
}

@EnabledIf(LinuxOnlyCondition::class)
class LinuxLowLevelKeyboardTest : LowLevelKeyboardTest() {

  @BeforeEach
  fun setupTests() {
    Configuration.browserPosition = "200x200"
    Configuration.browserSize = "200x200"
  }

  @Test
  fun `numpad Enter should be pressed and released on Linux`() = test("\n") {
    Runtime.getRuntime().exec("xdotool key KP_Enter").waitFor()
  }

  @Test
  fun `numpad should be pressed and released with num lock on Linux`() = test("5") {
    Runtime.getRuntime().exec("numlockx on").waitFor()
    keyPress(KeyEvent.VK_NUMPAD5)
    keyRelease(KeyEvent.VK_NUMPAD5)
  }

  @Test
  @Ignore  // todo: https://youtrack.jetbrains.com/issue/PRJ-301
  fun `numpad should be pressed and released without numlock on Linux`() = test("") {
    Runtime.getRuntime().exec("numlockx off").waitFor()
    keyPress(KeyEvent.VK_NUMPAD7)
    keyRelease(KeyEvent.VK_NUMPAD7)
  }

  @Test
  @Ignore  // todo: https://youtrack.jetbrains.com/issue/PRJ-194
  fun `Linux alt code should be typed`() = test("–") {
    keyPress(KeyEvent.VK_CONTROL)
    keyPress(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_U)
    keyRelease(KeyEvent.VK_U)
    keyPress(KeyEvent.VK_2)
    keyRelease(KeyEvent.VK_2)
    keyPress(KeyEvent.VK_0)
    keyRelease(KeyEvent.VK_0)
    keyPress(KeyEvent.VK_1)
    keyRelease(KeyEvent.VK_1)
    keyPress(KeyEvent.VK_3)
    keyRelease(KeyEvent.VK_3)
    keyRelease(KeyEvent.VK_SHIFT)
    keyRelease(KeyEvent.VK_CONTROL)
  }
}
