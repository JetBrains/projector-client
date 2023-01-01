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
package org.jetbrains.projector.client.web.debug

import kotlinx.browser.document
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.externalDeclarartion.pointerEvents
import org.w3c.dom.HTMLDivElement
import kotlin.math.roundToInt

interface PingShower {

  fun onHandshakeFinished()

  fun onPingReply(clientToServerMs: Int, serverToClientMs: Int, totalMs: Int)

  fun onClose()
}

object NoPingShower : PingShower {

  override fun onHandshakeFinished() {}

  override fun onPingReply(clientToServerMs: Int, serverToClientMs: Int, totalMs: Int) {}

  override fun onClose() {}
}

class DivPingShower : PingShower {

  private var div: HTMLDivElement? = null

  @OptIn(ExperimentalStdlibApi::class)
  private val pings = ArrayDeque<Int>()

  override fun onHandshakeFinished() {
    div = (document.createElement("div") as HTMLDivElement).apply {
      style.background = DEFAULT_BACKGROUND
      style.position = "fixed"
      style.top = "50%"
      style.left = "5%"
      style.padding = "5px"
      style.zIndex = "1"
      style.pointerEvents = "none"

      document.body!!.appendChild(this)
    }
  }

  @OptIn(ExperimentalStdlibApi::class)
  override fun onPingReply(clientToServerMs: Int, serverToClientMs: Int, totalMs: Int) {
    div?.let {
      it.innerText = """
        |Client -> Server: $clientToServerMs ms
        |Server -> Client: $serverToClientMs ms
        |Total: $totalMs ms
      """.trimMargin()

      val pingAverageCount = ParamsProvider.PING_AVERAGE_COUNT

      if (pingAverageCount != null) {
        pings.addLast(totalMs)

        val extra = pings.size - pingAverageCount

        if (extra > 0) {
          repeat(extra) { pings.removeFirst() }
        }

        if (pings.isNotEmpty()) {
          val averagePing = (pings.sum().toDouble() / pings.size).roundToInt()

          it.innerText += """
            |
            |Min: ${pings.minOrNull()!!} ms
            |Max: ${pings.maxOrNull()!!} ms
            |Average: $averagePing ms
          """.trimMargin()
        }
      }
    }
  }

  override fun onClose() {
    div?.remove()
    div = null
  }

  companion object {
    private const val DEFAULT_BACKGROUND = "rgba(25, 225, 225, 0.5)"
  }
}
