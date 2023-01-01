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

import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AverageTest {

  @Test
  fun testEmpty() {
    val stats = Average.createForDouble(DEFAULT_NAME, DEFAULT_ROUNDING_STRATEGY)

    val actual = stats.reset().data

    assertTrue(actual is Average.Data.Empty, "at the beginning there must be empty data")
  }

  @Test
  fun testByIterationsList() {
    val stats = Average.createForDouble(DEFAULT_NAME, DEFAULT_ROUNDING_STRATEGY)

    val data = listOf(0.0, 0.1, 0.1, 0.2, 1000.0)

    data.forEach(stats::add)

    val expectedAverage = data.sum() / data.size

    val resetResult = stats.reset()

    val resetData = resetResult.data
    assertTrue(resetData is Average.Data.Success, "extracted data after addition must be successful")

    val (average, iterations) = resetData
    assertEquals(data.size.toLong(), iterations, "iterations must be calculated properly")
    assertTrue((expectedAverage - average).absoluteValue < 1e-6, "average must be calculated properly")
  }

  @Test
  fun testByIterationsListReset() {
    val stats = Average.createForDouble(DEFAULT_NAME, DEFAULT_ROUNDING_STRATEGY)

    val data = listOf(0.0, 0.1, 0.1, 0.2, 1000.0)

    data.forEach(stats::add)

    stats.reset()
    val actual = stats.reset().data

    assertTrue(actual is Average.Data.Empty, "second reset in a row must give empty data")
  }

  companion object {

    private const val DEFAULT_NAME = "test"
    private val DEFAULT_ROUNDING_STRATEGY = RoundingStrategy.Multiplier
  }
}
