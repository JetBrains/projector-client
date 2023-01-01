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
package org.jetbrains.projector.client.web.input.key

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.projector.client.web.input.key.util.assertEqualsWithoutTimeStamp
import org.jetbrains.projector.client.web.input.key.util.toEvent
import org.jetbrains.projector.common.protocol.data.VK
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.DOWN
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.UP
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.LEFT
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.STANDARD
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.w3c.dom.events.Event
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ImeTest {

  @Test
  fun simpleQwerty() = runTest {
    // type "abc"

    val initial = """
      keydown: 5800763 a KeyA false 65 undefined
      input: 5800777 undefined undefined false undefined a
      keyup: 5800888 a KeyA false 65 undefined
      keydown: 5801123 b KeyB false 66 undefined
      input: 5801131 undefined undefined false undefined b
      keyup: 5801216 b KeyB false 66 undefined
      keydown: 5801404 c KeyC false 67 undefined
      input: 5801416 undefined undefined false undefined c
      keyup: 5801467 c KeyC false 67 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(9, initial.size)

    val expected = listOf(
      ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
      ClientKeyPressEvent(42, 'a', emptySet()),
      ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), UP),
      ClientKeyEvent(42, 'b', VK.B, STANDARD, emptySet(), DOWN),
      ClientKeyPressEvent(42, 'b', emptySet()),
      ClientKeyEvent(42, 'b', VK.B, STANDARD, emptySet(), UP),
      ClientKeyEvent(42, 'c', VK.C, STANDARD, emptySet(), DOWN),
      ClientKeyPressEvent(42, 'c', emptySet()),
      ClientKeyEvent(42, 'c', VK.C, STANDARD, emptySet(), UP),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  @Ignore
  fun windowsAltEnDash() = runTest {
    // type Alt + 0150

    val initial = """
      keydown: 1279940 Alt AltLeft false 18 undefined
      keydown: 1280282 0 Numpad0 false 96 undefined
      keyup: 1280360 0 Numpad0 false 96 undefined
      keydown: 1280549 1 Numpad1 false 97 undefined
      keyup: 1280626 1 Numpad1 false 97 undefined
      keydown: 1280767 5 Numpad5 false 101 undefined
      keyup: 1280846 5 Numpad5 false 101 undefined
      keydown: 1280986 0 Numpad0 false 96 undefined
      keyup: 1281065 0 Numpad0 false 96 undefined
      keyup: 1281251 Alt AltLeft false 18 undefined
      input: 1281259 undefined undefined false undefined –
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(11, initial.size)

    val expected = listOf(
      // todo
      ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
      ClientKeyPressEvent(42, 'a', emptySet()),
      ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), UP),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  @Ignore
  fun macAltEnDash() = runTest {
    // type Alt(Option) + Minus

    val initial = """
      keydown: 4698 Alt AltLeft false 18 undefined
      keydown: 4874 – Minus false 189 undefined
      input: 4882 undefined undefined false undefined –
      keyup: 4954 – Minus false 189 undefined
      keyup: 5106 Alt AltLeft false 18 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(5, initial.size)

    val expected = listOf(
      // todo
      ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), DOWN),
      ClientKeyPressEvent(42, 'a', emptySet()),
      ClientKeyEvent(42, 'a', VK.A, STANDARD, emptySet(), UP),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  fun linuxAltEnDash() = runTest {
    // type Ctrl + Shift + u + 2013

    val initial = """
      keydown: 113889 Control ControlLeft false 17 undefined
      keydown: 114088 Shift ShiftLeft false 16 undefined
      keydown: 114520 Process KeyU false 229 undefined
      compositionstart: 114520 undefined undefined false undefined 
      compositionupdate: 114520 undefined undefined false undefined u
      input: 114528 undefined undefined false undefined u
      keyup: 114621 U KeyU false 85 undefined
      keydown: 115300 Process Digit2 false 229 undefined
      compositionupdate: 115300 undefined undefined false undefined u2
      input: 115308 undefined undefined false undefined u2
      keyup: 115436 @ Digit2 false 50 undefined
      keydown: 115696 Process Digit0 false 229 undefined
      compositionupdate: 115696 undefined undefined false undefined u20
      input: 115705 undefined undefined false undefined u20
      keyup: 115793 ) Digit0 false 48 undefined
      keydown: 115891 Process Digit1 false 229 undefined
      compositionupdate: 115891 undefined undefined false undefined u201
      input: 115898 undefined undefined false undefined u201
      keyup: 116000 ! Digit1 false 49 undefined
      keydown: 116138 Process Digit3 false 229 undefined
      compositionupdate: 116139 undefined undefined false undefined u2013
      input: 116144 undefined undefined false undefined u2013
      keyup: 116258 # Digit3 false 51 undefined
      keyup: 116348 Process ShiftLeft false 229 undefined
      compositionupdate: 116348 undefined undefined false undefined 
      input: 116360 undefined undefined false undefined 
      compositionend: 116348 undefined undefined false undefined 
      input: 116362 undefined undefined false undefined 
      compositionstart: 116348 undefined undefined false undefined 
      compositionupdate: 116348 undefined undefined false undefined –
      compositionend: 116348 undefined undefined false undefined –
      input: 116385 undefined undefined false undefined –
      keyup: 116353 Control ControlLeft false 17 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(33, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, '–', emptySet()),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.CONTROL, LEFT, emptySet(), DOWN),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), DOWN),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  fun windowsChinese() = runTest {
    // type "zhongwen" one by one char and type space
    // kudos to https://youtrack.jetbrains.com/issue/PRJ-514#focus=Comments-27-5016836.0-0

    val initial = """
      keydown: 207435.25999994017 Process KeyZ false 229 undefined
      compositionstart: 207439.85500000417 undefined undefined false undefined 
      compositionupdate: 207440.14500000048 undefined undefined false undefined z
      input: 207442.02499999665 undefined undefined false undefined z
      compositionupdate: 207444.6000000462 undefined undefined false undefined z
      input: 207446.9500000123 undefined undefined false undefined z
      keyup: 207552.44500003755 Process KeyZ false 229 undefined
      keydown: 207706.4750000136 Process KeyH false 229 undefined
      compositionupdate: 207774.5199999772 undefined undefined false undefined zh
      input: 207781.39999997802 undefined undefined false undefined zh
      compositionupdate: 207785.84000002593 undefined undefined false undefined zh
      input: 207787.8250000067 undefined undefined false undefined zh
      keyup: 207798.42999996617 Process KeyH false 229 undefined
      keydown: 208061.7899999488 Process KeyO false 229 undefined
      compositionupdate: 208118.30999993253 undefined undefined false undefined zho
      input: 208132.89999996778 undefined undefined false undefined zho
      compositionupdate: 208138.99000000674 undefined undefined false undefined zho
      input: 208143.88500002678 undefined undefined false undefined zho
      keyup: 208131.50499993935 Process KeyO false 229 undefined
      keydown: 208273.58000003733 Process KeyN false 229 undefined
      compositionupdate: 208325.24499995634 undefined undefined false undefined zhon
      input: 208335.64499998465 undefined undefined false undefined zhon
      compositionupdate: 208341.23999997973 undefined undefined false undefined zhon
      input: 208348.54000003543 undefined undefined false undefined zhon
      keyup: 208368.2999999728 Process KeyN false 229 undefined
      keydown: 208502.08500004373 Process KeyG false 229 undefined
      compositionupdate: 208536.97499993723 undefined undefined false undefined zhong
      input: 208543.8650000142 undefined undefined false undefined zhong
      compositionupdate: 208549.83000003267 undefined undefined false undefined zhong
      input: 208555.3249999648 undefined undefined false undefined zhong
      keyup: 208572.46499997564 Process KeyG false 229 undefined
      keydown: 208724.67499994673 Process KeyW false 229 undefined
      compositionupdate: 208819.47999994736 undefined undefined false undefined zhong'w
      input: 208826.849999954 undefined undefined false undefined zhong'w
      compositionupdate: 208839.61000002455 undefined undefined false undefined zhong'w
      input: 208846.73500002827 undefined undefined false undefined zhong'w
      keydown: 208827.90000003297 Process KeyE false 229 undefined
      compositionupdate: 208882.07499997225 undefined undefined false undefined zhong'we
      input: 208886.39500003774 undefined undefined false undefined zhong'we
      compositionupdate: 208888.84499995038 undefined undefined false undefined zhong'we
      input: 208891.24999998603 undefined undefined false undefined zhong'we
      keyup: 208883.18500004243 Process KeyW false 229 undefined
      keydown: 208952.09999999497 Process KeyN false 229 undefined
      keyup: 208953.0650000088 Process KeyE false 229 undefined
      compositionupdate: 209005.09500002954 undefined undefined false undefined zhong'wen
      input: 209010.34999999683 undefined undefined false undefined zhong'wen
      compositionupdate: 209016.82000001892 undefined undefined false undefined zhong'wen
      input: 209022.03999995254 undefined undefined false undefined zhong'wen
      keyup: 209016.78499998525 Process KeyN false 229 undefined
      keyup: 209017.344999942 n KeyN false 78 undefined
      keydown: 209332.1500000311 Process Space false 229 undefined
      compositionupdate: 209354.13999995217 undefined undefined false undefined 中文
      input: 209367.66500002705 undefined undefined false undefined 中文
      compositionupdate: 209381.03499996942 undefined undefined false undefined 中文
      input: 209390.78499993775 undefined undefined false undefined 中文
      compositionend: 209391.02999994066 undefined undefined false undefined 中文
      keyup: 209450.69500000682 Process Space false 229 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(57, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, '中', emptySet()),
      ClientKeyPressEvent(42, '文', emptySet()),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  fun macSafariChinese() = runTest {
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
  fun macChromiumChinese() = runTest {
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

  @Test
  fun macSafariBrazilian1() = runTest {
    // a composition of SHIFT + ' (to get ^), release everything and then type u

    val initial = """
      keydown: 1923870.0000000002 Shift ShiftLeft false 16 undefined
      compositionstart: 1924139.0000000002 undefined undefined false undefined 
      compositionupdate: 1924140 undefined undefined false undefined ^
      input: 1924143 undefined undefined false undefined ^
      keydown: 1924126 Dead Quote false 229 undefined
      keyup: 1924222 " Quote false 222 undefined
      keyup: 1924406 Shift ShiftLeft false 16 undefined
      input: 1925279 undefined undefined false undefined null
      input: 1925281 undefined undefined false undefined û
      compositionend: 1925282.0000000002 undefined undefined false undefined û
      keydown: 1925262 û KeyU false 229 undefined
      keyup: 1925318 u KeyU false 85 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(12, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, 'û', emptySet()),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), DOWN),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), UP),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  fun macChromiumBrazilian1() = runTest {
    // a composition of SHIFT + ' (to get ^), release everything and then type u

    val initial = """
      keydown: 1585562.595 Shift ShiftLeft false 16 undefined
      keydown: 1585810.4449999996 Dead Quote false 229 undefined
      compositionstart: 1585812.4149999998 undefined undefined false undefined 
      compositionupdate: 1585812.52 undefined undefined false undefined ^
      input: 1585813.1550000003 undefined undefined false undefined ^
      keyup: 1585882.4449999998 Dead Quote false 222 undefined
      keyup: 1585986.4199999997 Shift ShiftLeft false 16 undefined
      keydown: 1587714.6100000003 û KeyU false 229 undefined
      compositionupdate: 1587719.1749999998 undefined undefined false undefined û
      input: 1587720.315 undefined undefined false undefined û
      compositionend: 1587720.3649999998 undefined undefined false undefined û
      keyup: 1587770.5549999997 u KeyU false 85 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(12, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, 'û', emptySet()),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), DOWN),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), UP),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  fun macSafariBrazilian2() = runTest {
    // a composition of SHIFT + ' (to get ^), release everything and then type space

    val initial = """
      keydown: 3287995 Shift ShiftLeft false 16 undefined
      compositionstart: 3288323 undefined undefined false undefined 
      compositionupdate: 3288323 undefined undefined false undefined ^
      input: 3288325.0000000005 undefined undefined false undefined ^
      keydown: 3288259 Dead Quote false 229 undefined
      keyup: 3288339 " Quote false 222 undefined
      keyup: 3288451 Shift ShiftLeft false 16 undefined
      input: 3288830 undefined undefined false undefined null
      input: 3288833 undefined undefined false undefined ˆ
      compositionend: 3288834.0000000005 undefined undefined false undefined ˆ
      keydown: 3288763 ˆ Space false 229 undefined
      keyup: 3288835 | Space false 32 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(12, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, 'ˆ', emptySet()),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), DOWN),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), UP),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  fun macChromiumBrazilian2() = runTest {
    // a composition of SHIFT + ' (to get ^), release everything and then type space

    val initial = """
      keydown: 3026658.5900000003 Shift ShiftLeft false 16 undefined
      keydown: 3027026.5999999996 Dead Quote false 229 undefined
      compositionstart: 3027030.0549999997 undefined undefined false undefined 
      compositionupdate: 3027030.235 undefined undefined false undefined ^
      input: 3027031.275 undefined undefined false undefined ^
      keyup: 3027106.5650000004 Dead Quote false 222 undefined
      keyup: 3027314.5850000004 Shift ShiftLeft false 16 undefined
      keydown: 3027986.585 ˆ Space false 229 undefined
      compositionupdate: 3027990.88 undefined undefined false undefined ˆ
      input: 3027992.0450000004 undefined undefined false undefined ˆ
      compositionend: 3027992.095 undefined undefined false undefined ˆ
      keyup: 3028042.535 | Space false 32 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(12, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, 'ˆ', emptySet()),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), DOWN),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), UP),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  @Ignore
  fun macSafariBrazilian3() = runTest {
    // a composition of SHIFT + ' (to get ^), release everything and then type d

    val initial = """
      keydown: 3441023 Shift ShiftLeft false 16 undefined
      compositionstart: 3441315 undefined undefined false undefined 
      compositionupdate: 3441315 undefined undefined false undefined ^
      input: 3441317 undefined undefined false undefined ^
      keydown: 3441247.0000000005 Dead Quote false 229 undefined
      keyup: 3441335 " Quote false 222 undefined
      keyup: 3441471 Shift ShiftLeft false 16 undefined
      input: 3441822 undefined undefined false undefined null
      input: 3441824 undefined undefined false undefined ^
      compositionend: 3441825.0000000005 undefined undefined false undefined ^
      keydown: 3441751 ^d KeyD false 68 undefined
      input: 3441834.0000000005 undefined undefined false undefined d
      keyup: 3441831 d KeyD false 68 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(13, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, '^', emptySet()),
      ClientKeyPressEvent(42, 'd', emptySet()),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), DOWN),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), UP),
    )

    handleEventsAndTest(initial, expected)
  }

  @Test
  fun macChromiumBrazilian3() = runTest {
    // a composition of SHIFT + ' (to get ^), release everything and then type d

    val initial = """
      keydown: 3141562.5999999996 Shift ShiftLeft false 16 undefined
      keydown: 3141810.5600000005 Dead Quote false 229 undefined
      compositionstart: 3141813.92 undefined undefined false undefined 
      compositionupdate: 3141814.1 undefined undefined false undefined ^
      input: 3141815.045 undefined undefined false undefined ^
      keyup: 3141898.57 Dead Quote false 222 undefined
      keyup: 3141986.58 Shift ShiftLeft false 16 undefined
      keydown: 3142298.585 d KeyD false 229 undefined
      compositionupdate: 3142302.875 undefined undefined false undefined ^d
      input: 3142303.8950000005 undefined undefined false undefined ^d
      compositionend: 3142303.9450000003 undefined undefined false undefined ^d
      keyup: 3142394.5850000004 d KeyD false 68 undefined
    """.trimIndent().lines().map(String::toEvent)

    assertEquals(12, initial.size)

    val expected = listOf(
      ClientKeyPressEvent(42, '^', emptySet()),
      ClientKeyPressEvent(42, 'd', emptySet()),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), DOWN),
      ClientKeyEvent(42, 0xFFFF.toChar(), VK.SHIFT, LEFT, emptySet(), UP),
    )

    handleEventsAndTest(initial, expected)
  }

  private companion object {

    private suspend fun handleEventsAndTest(
      initial: List<Event>,
      expected: List<ClientEvent>,
    ) {
      val actual = mutableListOf<ClientEvent>()

      val handler = ImeInputMethodEventHandler(42, actual::add, clearInputField = {})
      initial.forEach(handler::handleEvent)

      delay(1000)

      assertEqualsWithoutTimeStamp(expected, actual)
    }

    // https://youtrack.jetbrains.com/issue/KT-22228#focus=Comments-27-3964713.0-0
    fun runTest(block: suspend () -> Unit): dynamic = GlobalScope.promise { block() }
  }
}
