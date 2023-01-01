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
package org.jetbrains.projector.client.web.misc

import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.common.statistics.Average
import org.jetbrains.projector.common.statistics.Rate
import org.jetbrains.projector.common.statistics.RoundingStrategy
import org.jetbrains.projector.util.logging.Logger

object ClientStats {

  private val logger = Logger<ClientStats>()

  val simplePingAverage = Average.createForDouble("simple ping", RoundingStrategy.FractionDigits(2))

  val toClientMessageSizeAverage = Average.createForLong("to-client message size", RoundingStrategy.Multiplier, "bytes")
  val toClientMessageSizeRate = Rate.createForLong("to-client message size", RoundingStrategy.Multiplier, "bytes")

  val toClientCompressionRatioAverage = Average.createForDouble("to-client compression ratio", RoundingStrategy.FractionDigits(2))

  val drawEventCountAverage = Average.createForLong("draw event count", RoundingStrategy.Multiplier)
  val drawEventCountRate = Rate.createForLong("draw event count", RoundingStrategy.Multiplier)

  val decompressingTimeMsAverage = Average.createForDouble("decompressing time", RoundingStrategy.FractionDigits(2), "ms")
  val decompressingTimeMsRate = Rate.createForDouble("decompressing time", RoundingStrategy.FractionDigits(2), "ms")

  val decodingTimeMsAverage = Average.createForDouble("decoding time", RoundingStrategy.FractionDigits(2), "ms")
  val decodingTimeMsRate = Rate.createForDouble("decoding time", RoundingStrategy.FractionDigits(2), "ms")

  val drawingTimeMsAverage = Average.createForDouble("drawing time", RoundingStrategy.FractionDigits(2), "ms")
  val drawingTimeMsRate = Rate.createForDouble("drawing time", RoundingStrategy.FractionDigits(2), "ms")

  val otherProcessingTimeMsAverage = Average.createForDouble("other processing time", RoundingStrategy.FractionDigits(2), "ms")
  val otherProcessingTimeMsRate = Rate.createForDouble("other processing time", RoundingStrategy.FractionDigits(2), "ms")

  // it's sum of the previous stats just for convenience
  val totalTimeMsAverage = Average.createForDouble("total (sum) time", RoundingStrategy.FractionDigits(2), "ms")
  val totalTimeMsRate = Rate.createForDouble("total (sum) time", RoundingStrategy.FractionDigits(2), "ms")

  private val averageList = listOf(
    simplePingAverage,
    toClientMessageSizeAverage,
    toClientCompressionRatioAverage,
    drawEventCountAverage,
    decompressingTimeMsAverage,
    decodingTimeMsAverage,
    drawingTimeMsAverage,
    otherProcessingTimeMsAverage,
    totalTimeMsAverage
  )

  private val rateList = listOf(
    toClientMessageSizeRate,
    drawEventCountRate,
    decompressingTimeMsRate,
    decodingTimeMsRate,
    drawingTimeMsRate,
    otherProcessingTimeMsRate,
    totalTimeMsRate
  )

  fun printStats() {
    logger.info {
      val averageStats = averageList
        .map(Average<*>::reset)
        .joinToString("\n") { it.generateString(SEPARATOR) }

      val rateStats = rateList
        .map { it.reset(TimeStamp.current) }
        .joinToString("\n") { it.generateString(SEPARATOR) }

      """
        |Stats:
        |
        |$averageStats
        |
        |$rateStats
        |
        |Stats are reset!
      """.trimMargin()
    }
  }

  fun resetStats(timeStamp: Double) {
    averageList.forEach { it.reset() }
    rateList.forEach { it.reset(timeStamp) }
  }

  private const val SEPARATOR = "\t"
}
