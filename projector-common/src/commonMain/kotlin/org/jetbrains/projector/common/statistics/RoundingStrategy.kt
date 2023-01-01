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

import org.jetbrains.projector.common.misc.toString
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

sealed class RoundingStrategy {

  abstract fun round(value: Double): String


  class FractionDigits(private val fractionDigitsToLeave: Int) : RoundingStrategy() {

    override fun round(value: Double): String = value.toString(fractionDigitsToLeave)
  }

  object Multiplier : RoundingStrategy() {

    override fun round(value: Double): String {
      // todo: support more coefficients and provide logic for fractional numbers
      val digitCount = value.absoluteValue.roundToLong().toString().length

      val int = value.roundToLong()

      val (coefficient, degree) = coefficientToDegreeAsc
                                    .lastOrNull { (_, degree) -> degree < digitCount }
                                  ?: noCoefficientToDegree

      return "${int.toString().dropLast(degree)}$coefficient"
    }

    private val noCoefficientToDegree = "" to 0

    private val coefficientToDegreeAsc = listOf(
      "K" to 3,
      "M" to 6
    )
  }
}
