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
package org.jetbrains.projector.server.core.convert

import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.common.protocol.toClient.ServerDrawStringEvent
import org.jetbrains.projector.common.protocol.toClient.ServerSetFontEvent
import org.jetbrains.projector.server.core.convert.toClient.shrinkEvents
import kotlin.test.Test
import kotlin.test.assertEquals

class ToClientTransformShrinkEvents {

  @Test
  fun testSimple() {
    // simple -> same simple

    val initial = listOf(
      ServerDrawCommandsEvent.Target.Onscreen(1) to listOf(
        ServerSetFontEvent(10, 12),
        ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
      ),
    )
    val actual = initial.shrinkEvents()
    val expected = listOf(
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          ServerSetFontEvent(10, 12),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
        ),
      ),
    )
    assertEquals(expected, actual)
  }

  @Test
  fun testEmptyCommands() {
    // with empty commands -> they are skipped

    val initial = listOf(
      ServerDrawCommandsEvent.Target.Onscreen(1) to listOf(
        ServerSetFontEvent(10, 12),
        ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
      ),
      ServerDrawCommandsEvent.Target.Onscreen(2) to listOf(
      ),
      ServerDrawCommandsEvent.Target.Onscreen(3) to listOf(
        ServerSetFontEvent(10, 12),
        ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
      ),
    )
    val actual = initial.shrinkEvents()
    val expected = listOf(
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          ServerSetFontEvent(10, 12),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
        ),
      ),
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(3),
        listOf(
          ServerSetFontEvent(10, 12),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
        ),
      ),
    )
    assertEquals(expected, actual)
  }

  @Test
  fun testRepeatingSameTargets() {
    // repeating targets -> merged into single target

    val initial = listOf(
      ServerDrawCommandsEvent.Target.Onscreen(1) to listOf(
        ServerSetFontEvent(10, 12),
        ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
      ),
      ServerDrawCommandsEvent.Target.Onscreen(1) to listOf(
        ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
      ),
    )
    val actual = initial.shrinkEvents()
    val expected = listOf(
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          ServerSetFontEvent(10, 12),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
        ),
      ),
    )
    assertEquals(expected, actual)
  }

  @Test
  fun redundantStateEvents() {
    // redundant state changers -> they are skipped

    val initial = listOf(
      ServerDrawCommandsEvent.Target.Onscreen(1) to listOf(
        ServerSetFontEvent(10, 12),
        ServerSetFontEvent(10, 120),
        ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
      ),
    )
    val actual = initial.shrinkEvents()
    val expected = listOf(
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          ServerSetFontEvent(10, 120),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
        ),
      ),
    )
    assertEquals(expected, actual)
  }
}
