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

import kotlinx.browser.window
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.client.web.debug.DivPingShower
import org.jetbrains.projector.client.web.debug.NoPingShower
import org.jetbrains.projector.common.protocol.toClient.ServerPingReplyEvent

class PingStatistics(
  private val openingTimeStamp: Int,
  private val requestPing: () -> Unit,
) {

  private val pingShower = when (ParamsProvider.SHOW_PING) {
    true -> DivPingShower()
    false -> NoPingShower
  }

  private var interval: Int? = null

  fun onHandshakeFinished() {
    interval = window.setInterval(
      handler = requestPing,
      timeout = ParamsProvider.PING_INTERVAL
    )

    pingShower.onHandshakeFinished()
  }

  fun onPingReply(pingReply: ServerPingReplyEvent) {
    val currentTimeStamp = TimeStamp.current.toInt() - openingTimeStamp

    val ping = currentTimeStamp - pingReply.clientTimeStamp

    ClientStats.simplePingAverage.add(ping.toDouble())

    val clientToServer = pingReply.serverReadEventTimeStamp - pingReply.clientTimeStamp
    val serverToClient = currentTimeStamp - pingReply.serverReadEventTimeStamp

    pingShower.onPingReply(clientToServer, serverToClient, ping)
  }

  fun onClose() {
    interval?.let { window.clearInterval(handle = it) }
    interval = null

    pingShower.onClose()
  }
}
