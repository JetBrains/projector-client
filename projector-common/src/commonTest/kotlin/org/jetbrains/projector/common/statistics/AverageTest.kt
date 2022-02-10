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
package org.jetbrains.projector.common.statistics

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.math.absoluteValue

class AverageTest : FunSpec() {

  init {
    lateinit var stats: Average<Double>

    beforeTest {
      stats = Average.createForDouble(DEFAULT_NAME, DEFAULT_ROUNDING_STRATEGY)
    }

    test("reset stats should be empty") {
      val actual = stats.reset().data

      withClue("at the beginning there must be empty data") {
        actual.shouldBeInstanceOf<Average.Data.Empty>()
      }
    }

    test("iterations list should be successful after data addition") {
      val data = listOf(0.0, 0.1, 0.1, 0.2, 1000.0)

      data.forEach(stats::add)

      val expectedAverage = data.sum() / data.size

      val resetResult = stats.reset()

      val resetData = resetResult.data

      withClue("extracted data after addition must be successful") {
        resetData.shouldBeInstanceOf<Average.Data.Success>()

        val (average, iterations) = resetData

        withClue("iterations must be calculated properly") {
          iterations shouldBe data.size.toLong()
        }

        withClue("average must be calculated properly") {
          ((expectedAverage - average).absoluteValue < 1e-6).shouldBeTrue()
        }
      }
    }

    test("iterations list reset should be empty") {
      val data = listOf(0.0, 0.1, 0.1, 0.2, 1000.0)

      data.forEach(stats::add)

      stats.reset()
      val actual = stats.reset().data

      withClue("second reset in a row must give empty data") {
        actual.shouldBeInstanceOf<Average.Data.Empty>()
      }
    }
  }

  companion object {

    private const val DEFAULT_NAME = "test"
    private val DEFAULT_ROUNDING_STRATEGY = RoundingStrategy.Multiplier
  }
}
