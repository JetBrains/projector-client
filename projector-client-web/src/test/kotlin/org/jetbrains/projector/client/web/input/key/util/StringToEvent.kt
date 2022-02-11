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
package org.jetbrains.projector.client.web.input.key.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.w3c.dom.events.*

// Format:
// "${event.type}: ${event.timeStamp} ${event.key} ${event.code} ${event.defaultPrevented} ${event.keyCode} ${event.data}"
// Keylogger example: https://jsfiddle.net/wz1njr6p/
// Actual spaces are replaced with '|'
fun String.toEvent(): Event {
  val (type, others) = this.split(": ", limit = 2)
  val (timeStamp, key, code, defaultPrevented, keyCodeAndData) = others.split(" ", limit = 5)
  val (keyCode, data) = keyCodeAndData.split(" ", limit = 2)

  return when (type) {
    "compositionstart",
    "compositionupdate",
    "compositionend",
    -> CompositionEvent(type, CompositionEventInit(data = data))

    "input",
    -> InputEvent(type, InputEventInit(data = data))

    "keydown",
    "keyup",
    -> KeyboardEvent(type, KeyboardEventInit(key = key.replace('|', ' '), code = code).apply { asDynamic()["keyCode"] = keyCode.toInt() })

    else -> throw IllegalArgumentException("Unknown type '$type' for event '$this'")
  }
}

class StringToEventTest : FunSpec() {
  init {
    test("composition") {
      val initial = "compositionupdate: 7060.000000000001 undefined undefined false undefined z"
      val actual = initial.toEvent() as CompositionEvent

      actual.type shouldBe "compositionupdate"
      actual.data shouldBe "z"
    }

    test("input") {
      val initial = "input: 7061 undefined undefined false undefined z"
      val actual = initial.toEvent() as InputEvent

      actual.type shouldBe "input"
      actual.data shouldBe "z"
    }

    test("key") {
      val initial = "keyup: 7132.000000000001 z KeyZ false 90 undefined"
      val actual = initial.toEvent() as KeyboardEvent

      actual.key shouldBe "z"
      actual.code shouldBe "KeyZ"
      actual.keyCode shouldBe 90
    }
  }
}
