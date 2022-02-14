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
package org.jetbrains.projector.server.core.util

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.projector.common.protocol.toClient.Flush
import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.common.protocol.toClient.ServerDrawStringEvent
import org.jetbrains.projector.common.protocol.toClient.ServerSetFontEvent

class DistinctUpdatedOnscreenSurfacesTest : AnnotationSpec() {

  @Test
  fun testSimple() {
    // simple -> same simple

    val initial = listOf(
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          ServerSetFontEvent(10, 12),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
        ),
      ),
    )
    val actual = initial.distinctUpdatedOnscreenSurfaces()
    val expected = listOf(
      ServerDrawCommandsEvent.Target.Onscreen(1),
    )
    actual shouldBe expected
  }

  @Test
  fun testTwo() {
    // with two commands -> same two

    val initial = listOf(
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
    val actual = initial.distinctUpdatedOnscreenSurfaces()
    val expected = listOf(
      ServerDrawCommandsEvent.Target.Onscreen(1),
      ServerDrawCommandsEvent.Target.Onscreen(3),
    )
    actual shouldBe expected
  }

  @Test
  fun testRepeatingSameTargets() {
    // repeating targets -> merged into single target

    val initial = listOf(
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          ServerSetFontEvent(10, 12),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
        ),
      ),
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          ServerSetFontEvent(10, 12),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
        ),
      ),
    )
    val actual = initial.distinctUpdatedOnscreenSurfaces()
    val expected = listOf(
      ServerDrawCommandsEvent.Target.Onscreen(1),
    )
    actual shouldBe expected
  }

  @Test
  fun redundantStateEvents() {
    // redundant state changers -> they are skipped

    val initial = listOf(
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          ServerSetFontEvent(10, 120),
        ),
      ),
    )
    val actual = initial.distinctUpdatedOnscreenSurfaces()
    val expected = emptyList<ServerDrawCommandsEvent.Target.Onscreen>()
    actual shouldBe expected
  }

  @Test
  fun testFlush() {
    // flush should be skipped

    val initial = listOf(
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          ServerSetFontEvent(10, 12),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
          Flush,
        ),
      ),
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(2),
        listOf(
          Flush,
        ),
      ),
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(3),
        listOf(
          ServerSetFontEvent(10, 12),
          ServerDrawStringEvent("abc", 1.0, 2.0, 32.0),
        ),
      ),
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(1),
        listOf(
          Flush,
        ),
      ),
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(2),
        listOf(
          Flush,
        ),
      ),
      ServerDrawCommandsEvent(
        ServerDrawCommandsEvent.Target.Onscreen(3),
        listOf(
          Flush,
        ),
      ),
    )
    val actual = initial.distinctUpdatedOnscreenSurfaces()
    val expected = listOf(
      ServerDrawCommandsEvent.Target.Onscreen(1),
      ServerDrawCommandsEvent.Target.Onscreen(3),
    )
    actual shouldBe expected
  }
}
