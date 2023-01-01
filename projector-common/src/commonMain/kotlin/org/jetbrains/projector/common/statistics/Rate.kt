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

// todo: add tests
class Rate<ValueType : Number> private constructor(
  private val value: StatsValue<ValueType>,
  private val unit: String?,
  private val name: String,
  private val roundingStrategy: RoundingStrategy,
) {

  private var lastTimeStampMs: Double? = null

  fun add(measurement: ValueType) {
    // todo: synchronization is needed here
    value.add(measurement)
  }

  fun reset(currentTimeStampMs: Double): ResetResult {
    // todo: synchronization is needed here
    val lastMs = lastTimeStampMs

    lastTimeStampMs = currentTimeStampMs

    if (lastMs == null) {
      return ResetResult(Data.Empty)
    }

    val data = when (val delta = currentTimeStampMs - lastMs) {
      0.0 -> Data.Empty

      else -> {
        val resetValue = value.reset()

        Data.Success(resetValue.toDouble() / delta, delta)
      }
    }

    return ResetResult(data)
  }

  inner class ResetResult(val data: Data) {

    fun generateString(separator: String): String = "$name rate:$separator${data.generateString(roundingStrategy, unit, separator)}"
  }

  sealed class Data {

    abstract fun generateString(roundingStrategy: RoundingStrategy, unit: String?, separator: String): String

    object Empty : Data() {

      override fun generateString(roundingStrategy: RoundingStrategy, unit: String?, separator: String): String {
        return "no data${separator}(no delta data)"
      }
    }

    data class Success(val ratePerMs: Double, val deltaMs: Double) : Data() {

      override fun generateString(roundingStrategy: RoundingStrategy, unit: String?, separator: String): String {
        val postfix = when (unit) {
          null -> ""

          else -> " ${unit.trim()}"
        }

        return "${roundingStrategy.round(ratePerMs * 1000)}$postfix per second${separator}(${roundDeltaMs(deltaMs)})"
      }

      companion object {

        private val deltaRounding = RoundingStrategy.FractionDigits(1)

        private fun roundDeltaMs(deltaMs: Double) = "${deltaRounding.round(deltaMs / 1000)} seconds"
      }
    }
  }

  companion object {

    fun createForDouble(name: String, roundingStrategy: RoundingStrategy, unit: String? = null): Rate<Double> =
      Rate(DoubleValue(), unit, name, roundingStrategy)

    fun createForLong(name: String, roundingStrategy: RoundingStrategy, unit: String? = null): Rate<Long> =
      Rate(LongValue(), unit, name, roundingStrategy)
  }
}
