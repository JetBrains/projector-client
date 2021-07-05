/*
 * MIT License
 *
 * Copyright (c) 2019-2021 JetBrains s.r.o.
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

      else -> throw AssertionError("bad event types at index $i; actual list: $actual")
    }
  }
}

fun assertEqualsWithoutTimeStamp(expected: ClientKeyPressEvent, actual: ClientKeyPressEvent, message: String? = null) {
  assertEquals(expected.char, actual.char, message)
  assertEquals(expected.modifiers, actual.modifiers, message)
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
        listOf(ClientKeyEvent(42, 'a', VK.A, ClientKeyEvent.KeyLocation.STANDARD, emptySet(), ClientKeyEvent.KeyEventType.DOWN)),
      )
    }
  }

  @Test
  fun differentTimeStamp() {
    assertEqualsWithoutTimeStamp(
      ClientKeyPressEvent(42, 'a', emptySet()),
      ClientKeyPressEvent(11, 'a', emptySet()),
    )
  }

  @Test
  fun differentChar() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        ClientKeyPressEvent(42, 'a', emptySet()),
        ClientKeyPressEvent(42, 'b', emptySet()),
      )
    }
  }

  @Test
  fun differentModifiers() {
    assertFailsWith<AssertionError> {
      assertEqualsWithoutTimeStamp(
        ClientKeyPressEvent(42, 'a', emptySet()),
        ClientKeyPressEvent(42, 'a', setOf(KeyModifier.ALT_KEY)),
      )
    }
  }
}
