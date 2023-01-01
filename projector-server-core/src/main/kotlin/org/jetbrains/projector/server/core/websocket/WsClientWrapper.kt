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
package org.jetbrains.projector.server.core.websocket

import org.java_websocket.WebSocket
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.jetbrains.projector.server.core.ClientSettings
import org.jetbrains.projector.server.core.ClientWrapper
import org.jetbrains.projector.server.core.ClosedClientSettings
import org.jetbrains.projector.util.logging.Logger
import java.net.InetAddress

public class WsClientWrapper(private val connection: WebSocket, initialSettings: ClientSettings): ClientWrapper {
  override var settings: ClientSettings = initialSettings

  override fun disconnect(reason: String) {
    val normalClosureCode = 1000  // https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent#properties
    val conn = connection
    conn.close(normalClosureCode, reason)

    val clientSettings = conn.getAttachment<ClientSettings>()!!
    logger.info { "Disconnecting user ${clientSettings.address}. Reason: $reason" }
    conn.setAttachment(
      ClosedClientSettings(
        connectionMillis = clientSettings.connectionMillis,
        address = clientSettings.address,
        reason = reason,
      )
    )
  }

  override fun send(data: ByteArray) {
    try {
      connection.send(data) // can cause a "disconnected already" exception
    }
    catch (e: WebsocketNotConnectedException) {
      logger.debug(e) { "While generating message, client disconnected" }
    }
  }

  override val requiresConfirmation: Boolean
    get() {
      val remoteAddress = connection.remoteSocketAddress?.address
      return remoteAddress?.isLoopbackAddress != true
    }

  override val confirmationRemoteIp: InetAddress?
    get() = connection.remoteSocketAddress?.address

  override val confirmationRemoteName: String
    get() = "unknown host"

  private companion object {
    private val logger = Logger<WsClientWrapper>()
  }
}
