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
package org.jetbrains.projector.agent.init

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IjArgsTest : FunSpec({
                             val argString = "key1=stringValue;key2=2;key3=3.14;key4=false;key5=customObject"

                             test("Map to string") {
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

                               val testArgString = map.toIjArgs()

                               testArgString shouldBe argString
                             }

                             test("String to map") {
                               val map = argString.toArgsMap()

                               map.size shouldBe 5
                               map["key1"] shouldBe "stringValue"
                               map["key2"] shouldBe "2"
                               map["key3"] shouldBe "3.14"
                               map["key4"] shouldBe "false"
                               map["key5"] shouldBe "customObject"
                             }
                           })
