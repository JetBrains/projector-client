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
package org.jetbrains.projector.agent.init

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe

class IjArgsTest : AnnotationSpec() {

  @Test
  fun `map to string should be equal`() {

    val obj = object : Any() {
      override fun toString(): String {
        return "customObject"
      }
    }

    val map = mapOf(
      "key1" to "stringValue",
      "key2" to 2,
      "key3" to 3.14,
      "key4" to false,
      "key5" to obj,
    )

    val argString = map.toIjArgs()

    argString shouldBe "key1=stringValue;key2=2;key3=3.14;key4=false;key5=customObject"
  }

  @Test
  fun `string to map should be equal`() {

    val argString = "key1=stringValue;key2=2;key3=3.14;key4=false;key5=customObject"

    val map = argString.toArgsMap()

    map.shouldHaveSize(5)
    map.shouldContain("key1", "stringValue")
    map.shouldContain("key2", "2")
    map.shouldContain("key3", "3.14")
    map.shouldContain("key4", "false")
    map.shouldContain("key5", "customObject")
  }
}
