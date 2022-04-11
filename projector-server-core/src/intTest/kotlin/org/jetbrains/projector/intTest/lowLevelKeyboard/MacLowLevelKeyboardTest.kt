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

class MacOnlyCondition : EnabledCondition {
  override fun enabled(kclass: KClass<out Spec>): Boolean = when {
    kclass.simpleName?.contains("Linux") == true -> OS.MAC.isCurrentOs
    else -> true // non Mac tests always run
  }
}

@EnabledIf(MacOnlyCondition::class)
class MacLowLevelKeyboardTest : LowLevelKeyboardTest(){

  @BeforeEach
  fun setupTests() {
    Configuration.browserPosition = "200x200"
    Configuration.browserSize = "200x200"
  }


  @Test
  fun `Mac alt code should be typed`() = test("–") {
    keyPress(KeyEvent.VK_ALT)
    keyPress(KeyEvent.VK_MINUS)
    keyRelease(KeyEvent.VK_MINUS)
    keyRelease(KeyEvent.VK_ALT)
  }
}
