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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.shouldBe
import org.jetbrains.projector.common.protocol.data.VK
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.DOWN
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.UP
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.LEFT
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.STANDARD
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier

// todo: rewrite to custom matcher (https://kotest.io/docs/assertions/assertions.html#custom-matchers)
fun assertEqualsWithoutTimeStamp(expected: List<ClientEvent>, actual: List<ClientEvent>) {
  withClue("actual list: $actual") {
    actual.shouldBeSameSizeAs(expected)
  }
  expected.forEachIndexed { i, exp ->
    val act = actual[i]
    when {
      exp is ClientKeyPressEvent && act is ClientKeyPressEvent -> assertEqualsWithoutTimeStamp(exp, act, "difference in event $i")
      exp is ClientKeyEvent && act is ClientKeyEvent -> assertEqualsWithoutTimeStamp(exp, act, "difference in event $i")

      else -> throw AssertionError("bad event types at index $i; actual list: $actual")
    }
  }
}

fun assertEqualsWithoutTimeStamp(expected: ClientKeyPressEvent, actual: ClientKeyPressEvent, message: String? = null) {
  withClue(message) {
    actual.char shouldBe expected.char
    actual.modifiers shouldBe expected.modifiers
  }
}

fun assertEqualsWithoutTimeStamp(expected: ClientKeyEvent, actual: ClientKeyEvent, message: String? = null) {
  withClue(message) {
    actual.char shouldBe expected.char
    actual.code shouldBe expected.code
    actual.location shouldBe expected.location
    actual.modifiers shouldBe expected.modifiers
    actual.keyEventType shouldBe expected.keyEventType
  }
}

class AssertEqualsWithoutTimeStampTest : FunSpec() {
  init {
    test("different sizes should consider not equal") {
      shouldThrow<AssertionError> {
        assertEqualsWithoutTimeStamp(
          listOf(ClientKeyPressEvent(42, 'a', emptySet())),
          emptyList(),
        )
      }
    }

    test("different types should consider not equal") {
      shouldThrow<AssertionError> {
        assertEqualsWithoutTimeStamp(
          listOf(ClientKeyPressEvent(42, 'a', emptySet())),
          listOf(ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN)),
        )
      }
    }

    test("different TimeStamp for key press should consider not equal") {
      assertEqualsWithoutTimeStamp(
        ClientKeyPressEvent(42, 'a', emptySet()),
        ClientKeyPressEvent(11, 'a', emptySet()),
      )
    }

    test("different char for key press should consider not equal") {
      shouldThrow<AssertionError> {
        assertEqualsWithoutTimeStamp(
          ClientKeyPressEvent(42, 'a', emptySet()),
          ClientKeyPressEvent(42, 'b', emptySet()),
        )
      }
    }

    test("different modifiers for key press should consider not equal") {
      shouldThrow<AssertionError> {
        assertEqualsWithoutTimeStamp(
          ClientKeyPressEvent(42, 'a', emptySet()),
          ClientKeyPressEvent(42, 'a', setOf(KeyModifier.ALT_KEY)),
        )
      }
    }

    test("different TimeStamp for key should consider not equal") {
      assertEqualsWithoutTimeStamp(
        ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
        ClientKeyEvent(11, 'a', VK.A, STANDARD, emptySet(), DOWN),
      )
    }

    test("different char for key should consider not equal") {
      shouldThrow<AssertionError> {
        assertEqualsWithoutTimeStamp(
          ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
          ClientKeyEvent(42, 'b', VK.A, STANDARD, emptySet(), DOWN),
        )
      }
    }

    test("different code for key should consider not equal") {
      shouldThrow<AssertionError> {
        assertEqualsWithoutTimeStamp(
          ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
          ClientKeyEvent(42, 'a', VK.B, STANDARD, emptySet(), DOWN),
        )
      }
    }

    test("different location for key should consider not equal") {
      shouldThrow<AssertionError> {
        assertEqualsWithoutTimeStamp(
          ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
          ClientKeyEvent(42, 'a', VK.A, LEFT, emptySet(), DOWN),
        )
      }
    }

    test("different modifiers for key should consider not equal") {
      shouldThrow<AssertionError> {
        assertEqualsWithoutTimeStamp(
          ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
          ClientKeyEvent(42, 'a', VK.A, STANDARD, setOf(KeyModifier.ALT_KEY), DOWN),
        )
      }
    }

    test("different type for key should consider not equal") {
      shouldThrow<AssertionError> {
        assertEqualsWithoutTimeStamp(
          ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
          ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), UP),
        )
      }
    }
  }
}
