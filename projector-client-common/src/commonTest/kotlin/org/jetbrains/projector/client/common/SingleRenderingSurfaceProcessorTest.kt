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
package org.jetbrains.projector.client.common

import org.jetbrains.projector.client.common.SingleRenderingSurfaceProcessor.Companion.shrinkByPaintEvents
import org.jetbrains.projector.common.protocol.data.AlphaCompositeRule
import org.jetbrains.projector.common.protocol.data.CommonAlphaComposite
import org.jetbrains.projector.common.protocol.data.PaintValue
import org.jetbrains.projector.common.protocol.toClient.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SingleRenderingSurfaceProcessorTest {

  @Test
  fun testShrink0() {
    val initial = listOf(
      ServerDrawLineEvent(0, 1, 2, 3),
      ServerDrawStringEvent("abc", 1.0, 2.0, 20.0),
    )
    val actual = initial.shrinkByPaintEvents()
    val expected = listOf(
      StateAndPaint(
        prerequisites = emptyList(),
        paintEvent = ServerDrawLineEvent(0, 1, 2, 3),
      ),
      StateAndPaint(
        prerequisites = emptyList(),
        paintEvent = ServerDrawStringEvent("abc", 1.0, 2.0, 20.0),
      ),
    )
    assertEquals(expected, actual)
  }

  @Test
  fun testShrink1() {
    val initial = listOf(
      ServerSetPaintEvent(PaintValue.Color(0x1234_5678)),
      ServerSetFontEvent(2, 12),
      ServerDrawStringEvent("abc", 1.0, 2.0, 20.0),
      ServerSetPaintEvent(PaintValue.Color(0x5678_1234)),
      ServerSetCompositeEvent(CommonAlphaComposite(AlphaCompositeRule.SRC, 1f)),
      ServerDrawLineEvent(0, 1, 2, 3),
    )
    val actual = initial.shrinkByPaintEvents()
    val expected = listOf(
      StateAndPaint(
        prerequisites = listOf(
          ServerSetPaintEvent(PaintValue.Color(0x1234_5678)),
          ServerSetFontEvent(2, 12),
        ),
        paintEvent = ServerDrawStringEvent("abc", 1.0, 2.0, 20.0),
      ),
      StateAndPaint(
        prerequisites = listOf(
          ServerSetPaintEvent(PaintValue.Color(0x5678_1234)),
          ServerSetCompositeEvent(CommonAlphaComposite(AlphaCompositeRule.SRC, 1f)),
        ),
        paintEvent = ServerDrawLineEvent(0, 1, 2, 3),
      ),
    )
    assertEquals(expected, actual)
  }
}
