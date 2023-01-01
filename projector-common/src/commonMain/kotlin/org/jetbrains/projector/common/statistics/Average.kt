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

class Average<ValueType : Number> private constructor(
  private val value: StatsValue<ValueType>,
  private val unit: String?,
  private val name: String,
  private val roundingStrategy: RoundingStrategy,
) {

  private var iterations: Long = 0

  fun add(measurement: ValueType) {
    // todo: synchronization is needed here
    ++iterations
    value.add(measurement)
  }

  fun reset(): ResetResult {
    // todo: synchronization is needed here
    val data = when (iterations) {
      0L -> Data.Empty

      else -> {
        val resetValue = value.reset()

        Data.Success(resetValue.toDouble() / iterations, iterations).also { iterations = 0 }
      }
    }

    return ResetResult(data)
  }

  inner class ResetResult(val data: Data) {

    fun generateString(separator: String): String = "$name average:$separator${data.generateString(roundingStrategy, unit, separator)}"
  }

  sealed class Data {

    abstract fun generateString(roundingStrategy: RoundingStrategy, unit: String?, separator: String): String

    object Empty : Data() {

      override fun generateString(roundingStrategy: RoundingStrategy, unit: String?, separator: String): String {
        return "no data${separator}(no iterations data)"
      }
    }

    data class Success(val average: Double, val iterations: Long) : Data() {

      override fun generateString(roundingStrategy: RoundingStrategy, unit: String?, separator: String): String {
        val postfix = when (unit) {
          null -> ""

          else -> " ${unit.trim()}"
        }

        return "${roundingStrategy.round(average)}$postfix${separator}($iterations iterations)"
      }
    }
  }

  companion object {

    fun createForDouble(name: String, roundingStrategy: RoundingStrategy, unit: String? = null): Average<Double> =
      Average(DoubleValue(), unit, name, roundingStrategy)

    fun createForLong(name: String, roundingStrategy: RoundingStrategy, unit: String? = null): Average<Long> =
      Average(LongValue(), unit, name, roundingStrategy)
  }
}
