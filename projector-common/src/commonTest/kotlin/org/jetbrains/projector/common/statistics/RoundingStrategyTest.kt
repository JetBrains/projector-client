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
package org.jetbrains.projector.common.statistics

import kotlin.test.Test
import kotlin.test.assertEquals

class RoundingStrategyTest {

  @Test
  fun testFractionDigits() {
    val tests = listOf(
      Triple(11.2345, 0, "11"),
      Triple(11.2345, 1, "11.2"),
      Triple(11.2345, 2, "11.23"),
      Triple(11.2345, 3, "11.235"),
      Triple(11.2345, 4, "11.2345"),
      Triple(11.2345, 5, "11.23450")
    )

    tests.forEach { (number, fractionDigitsToLeave, expected) ->
      val strategy = RoundingStrategy.FractionDigits(fractionDigitsToLeave)

      assertEquals(expected, strategy.round(number), "FractionDigits($fractionDigitsToLeave).round($number) must be $expected")
    }
  }

  @Test
  fun testMultiplier() {
    val tests = listOf(
      Pair(0.1, "0"),
      Pair(1.1, "1"),
      Pair(11.1, "11"),
      Pair(111.1, "111"),
      Pair(1_111.1, "1K"),
      Pair(11_111.1, "11K"),
      Pair(111_111.1, "111K"),
      Pair(1_111_111.1, "1M"),
      Pair(11_111_111.1, "11M"),
      Pair(12_345_678.1, "12M"),
      Pair(978.1, "978")
    )

    tests.forEach { (number, expected) ->
      val strategy = RoundingStrategy.Multiplier

      assertEquals(expected, strategy.round(number), "Multiplier.round($number) must be $expected")
    }
  }
}
