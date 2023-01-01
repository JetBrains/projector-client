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
package org.jetbrains.projector.client.web.input.key.util

import org.jetbrains.projector.common.protocol.data.VK
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.DOWN
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.UP
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.LEFT
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.STANDARD
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

fun assertEqualsWithoutTimeStamp(expected: List<ClientEvent>, actual: List<ClientEvent>) {
  assertEquals(expected.size, actual.size, "actual list: $actual")
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
  assertEquals(expected.char, actual.char, message)
  assertEquals(expected.modifiers, actual.modifiers, message)
}

fun assertEqualsWithoutTimeStamp(expected: ClientKeyEvent, actual: ClientKeyEvent, message: String? = null) {
  assertEquals(expected.char, actual.char, message)
  assertEquals(expected.code, actual.code, message)
  assertEquals(expected.location, actual.location, message)
  assertEquals(expected.modifiers, actual.modifiers, message)
  assertEquals(expected.keyEventType, actual.keyEventType, message)
}

class AssertEqualsWithoutTimeStampTest {

  @Test
  fun differentSize() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        listOf(ClientKeyPressEvent(42, 'a', emptySet())),
        emptyList(),
      )
    }
  }

  @Test
  fun differentTypes() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        listOf(ClientKeyPressEvent(42, 'a', emptySet())),
        listOf(ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN)),
      )
    }
  }

  @Test
  fun differentTimeStampForKeyPress() {
    assertEqualsWithoutTimeStamp(
      ClientKeyPressEvent(42, 'a', emptySet()),
      ClientKeyPressEvent(11, 'a', emptySet()),
    )
  }

  @Test
  fun differentCharForKeyPress() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        ClientKeyPressEvent(42, 'a', emptySet()),
        ClientKeyPressEvent(42, 'b', emptySet()),
      )
    }
  }

  @Test
  fun differentModifiersForKeyPress() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        ClientKeyPressEvent(42, 'a', emptySet()),
        ClientKeyPressEvent(42, 'a', setOf(KeyModifier.ALT_KEY)),
      )
    }
  }

  @Test
  fun differentTimeStampForKey() {
    assertEqualsWithoutTimeStamp(
      ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
      ClientKeyEvent(11, 'a', VK.A, STANDARD, emptySet(), DOWN),
    )
  }

  @Test
  fun differentCharForKey() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
        ClientKeyEvent(42, 'b', VK.A, STANDARD, emptySet(), DOWN),
      )
    }
  }

  @Test
  fun differentCodeForKey() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
        ClientKeyEvent(42, 'a', VK.B, STANDARD, emptySet(), DOWN),
      )
    }
  }

  @Test
  fun differentLocationForKey() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
        ClientKeyEvent(42, 'a', VK.A, LEFT, emptySet(), DOWN),
      )
    }
  }

  @Test
  fun differentModifiersForKey() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
        ClientKeyEvent(42, 'a', VK.A, STANDARD, setOf(KeyModifier.ALT_KEY), DOWN),
      )
    }
  }

  @Test
  fun differentTypeForKey() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
        ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), UP),
      )
    }
  }
}
