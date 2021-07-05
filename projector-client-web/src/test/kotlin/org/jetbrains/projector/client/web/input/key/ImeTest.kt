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
package org.jetbrains.projector.client.web.input.key

import org.jetbrains.projector.client.web.input.key.util.assertEqualsWithoutTimeStamp
import org.jetbrains.projector.client.web.input.key.util.toEvent
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.w3c.dom.events.Event
import kotlin.test.Test
import kotlin.test.assertEquals

class ImeTest {

  @Test
  fun macSafariChinese() {
    // type "zhongwen" one by one char and type space

    val initial = """
      compositionstart: 7060.000000000001 undefined undefined false undefined 
      compositionupdate: 7060.000000000001 undefined undefined false undefined z
      input: 7061 undefined undefined false undefined z
      keydown: 7052.000000000001 z KeyZ false 229 undefined
      keyup: 7132.000000000001 z KeyZ false 90 undefined
      compositionupdate: 7243 undefined undefined false undefined zh
      input: 7244 undefined undefined false undefined zh
      keydown: 7236 h KeyH false 229 undefined
      keyup: 7308 h KeyH false 72 undefined
      compositionupdate: 7568.000000000001 undefined undefined false undefined zho
      input: 7570 undefined undefined false undefined zho
      keydown: 7564 o KeyO false 229 undefined
      keyup: 7636 o KeyO false 79 undefined
      compositionupdate: 7840 undefined undefined false undefined zhon
      input: 7842.000000000001 undefined undefined false undefined zhon
      keydown: 7836 n KeyN false 229 undefined
      keyup: 7924 n KeyN false 78 undefined
      compositionupdate: 8040.000000000001 undefined undefined false undefined zhong
      input: 8042 undefined undefined false undefined zhong
      keydown: 8036 g KeyG false 229 undefined
      keyup: 8116 g KeyG false 71 undefined
      compositionupdate: 8440 undefined undefined false undefined zhong w
      input: 8442 undefined undefined false undefined zhong w
      keydown: 8436 w KeyW false 229 undefined
      keyup: 8532 w KeyW false 87 undefined
      compositionupdate: 8993 undefined undefined false undefined zhong we
      input: 8995.000000000002 undefined undefined false undefined zhong we
      keydown: 8988 e KeyE false 229 undefined
      keyup: 9012 e KeyE false 69 undefined
      compositionupdate: 9168.000000000002 undefined undefined false undefined zhong wen
      input: 9170 undefined undefined false undefined zhong wen
      keydown: 9164 n KeyN false 229 undefined
      keyup: 9228 n KeyN false 78 undefined
      input: 10003 undefined undefined false undefined null
      input: 10005 undefined undefined false undefined 中文
      compositionend: 10007 undefined undefined false undefined 中文
      keydown: 9996 | Space false 229 undefined
      keyup: 10060 | Space false 32 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(38, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, '中', emptySet()),
      ClientKeyPressEvent(42, '文', emptySet()),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  fun macChromiumChinese() {
    // type "zhongwen" one by one char and type space

    val initial = """
      keydown: 35692.85499999978 z KeyZ false 229 undefined
      compositionstart: 35709.03499999986 undefined undefined false undefined 
      compositionupdate: 35709.239999999685 undefined undefined false undefined z
      input: 35709.875000000015 undefined undefined false undefined z
      keyup: 35764.68999999997 z KeyZ false 90 undefined
      keydown: 35940.85499999983 h KeyH false 229 undefined
      compositionupdate: 35952.704999999696 undefined undefined false undefined zh
      input: 35953.4749999998 undefined undefined false undefined zh
      keyup: 36004.840000000055 h KeyH false 72 undefined
      keydown: 36308.860000000095 o KeyO false 229 undefined
      compositionupdate: 36319.24499999968 undefined undefined false undefined zho
      input: 36320.22999999981 undefined undefined false undefined zho
      keyup: 36372.84499999987 o KeyO false 79 undefined
      keydown: 36588.84499999977 n KeyN false 229 undefined
      compositionupdate: 36597.499999999854 undefined undefined false undefined zhon
      input: 36598.52999999975 undefined undefined false undefined zhon
      keyup: 36660.85999999996 n KeyN false 78 undefined
      keydown: 36868.860000000044 g KeyG false 229 undefined
      compositionupdate: 36878.80999999971 undefined undefined false undefined zhong
      input: 36880.09499999999 undefined undefined false undefined zhong
      keyup: 36940.83999999975 g KeyG false 71 undefined
      keydown: 37924.845 w KeyW false 229 undefined
      compositionupdate: 37935.04499999972 undefined undefined false undefined zhong w
      input: 37936.7699999998 undefined undefined false undefined zhong w
      keyup: 38012.78999999977 w KeyW false 87 undefined
      keydown: 38116.85499999976 e KeyE false 229 undefined
      compositionupdate: 38127.74999999965 undefined undefined false undefined zhong we
      input: 38129.469999999856 undefined undefined false undefined zhong we
      keyup: 38180.80999999984 e KeyE false 69 undefined
      keydown: 38292.860000000015 n KeyN false 229 undefined
      compositionupdate: 38303.349999999686 undefined undefined false undefined zhong wen
      input: 38305.28999999979 undefined undefined false undefined zhong wen
      keyup: 38364.73999999998 n KeyN false 78 undefined
      keydown: 39108.859999999826 | Space false 229 undefined
      compositionupdate: 39117.39499999976 undefined undefined false undefined 中文
      input: 39120.15500000007 undefined undefined false undefined 中文
      compositionend: 39120.21000000004 undefined undefined false undefined 中文
      keyup: 39188.81999999985 | Space false 32 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(38, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, '中', emptySet()),
      ClientKeyPressEvent(42, '文', emptySet()),
    )

    handleEventsAndTest(initial, expected)
  }

  private companion object {

    private fun handleEventsAndTest(
      initial: List<Event>,
      expected: List<ClientKeyPressEvent>,
    ) {
      val actual = mutableListOf<ClientEvent>()

      val handler = ImeInputMethodEventHandler(42, actual::add, clearInputField = {})
      initial.forEach(handler::handleEvent)

      assertEqualsWithoutTimeStamp(expected, actual)
    }
  }
}
