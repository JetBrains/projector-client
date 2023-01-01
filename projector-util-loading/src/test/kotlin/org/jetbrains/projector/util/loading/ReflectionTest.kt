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
package org.jetbrains.projector.util.loading

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.lang.reflect.Modifier

class ReflectionTest : AnnotationSpec() {

  private class TestClass {
    @Suppress("unused")
    private val unprotectMe = 0
    @Suppress("unused")
    private var unprotectMeToo = 0
  }

  @Test
  fun `final field can be unprotected`() {
    runFieldUnprotectTest("unprotectMe", true)
  }

  @Test
  fun `field can be unprotected`() {
    runFieldUnprotectTest("unprotectMeToo", false)
  }

  private fun runFieldUnprotectTest(fieldName: String, isFinalByDefault: Boolean) {
    val field = TestClass::class.java.getDeclaredField(fieldName)
    val classInstance = TestClass()

    field.canAccess(classInstance).shouldBeFalse()
    Modifier.isFinal(field.modifiers) shouldBe isFinalByDefault

    field.unprotect()

    field.canAccess(classInstance).shouldBeTrue()
    Modifier.isFinal(field.modifiers).shouldBeFalse()
  }
}
