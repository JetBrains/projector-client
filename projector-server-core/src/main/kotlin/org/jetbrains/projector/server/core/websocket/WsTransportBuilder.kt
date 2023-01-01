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
import org.jetbrains.projector.server.core.ClientEventHandler
import org.jetbrains.projector.server.core.ClientWrapper
import org.jetbrains.projector.util.logging.Logger
import java.nio.ByteBuffer

public abstract class WsTransportBuilder {
  public lateinit var onError: (WebSocket?, Exception) -> Unit
  public lateinit var onWsOpen: (connection: WebSocket) -> Unit
  public lateinit var onWsClose: (connection: WebSocket) -> Unit
  public lateinit var onWsMessageString: (connection: WebSocket, message: String) -> Unit
  public lateinit var onWsMessageByteBuffer: (connection: WebSocket, message: ByteBuffer) -> Unit

  public abstract fun build(): HttpWsTransport

  public fun attachDefaultServerEventHandlers(clientEventHandler: ClientEventHandler): WsTransportBuilder {
    onWsMessageByteBuffer = { _, message ->
      throw RuntimeException("Unsupported message type: $message")
    }

    onWsMessageString = { connection, message ->
      clientEventHandler.handleMessage(connection.getAttachment(), message)
    }

    onWsClose = { connection ->
      // todo: we need more informative message, add parameters to this method inside the superclass
      clientEventHandler.updateClientsCount()
      val wrapper = connection.getAttachment<ClientWrapper>()
      if (wrapper != null) {
        clientEventHandler.onClientConnectionEnded(wrapper)
      } else {
        logger.info {
          val address = connection.remoteSocketAddress?.address?.hostAddress
          "Client from address $address is disconnected. This client hasn't clientSettings. " +
          "This usually happens when the handshake stage didn't have time to be performed " +
          "(so it seems the client has been connected for a very short time)"
        }
      }
    }

    onWsOpen = { connection ->
      val address = connection.remoteSocketAddress?.address?.hostAddress
      val wrapper = WsClientWrapper(connection, clientEventHandler.getInitialClientState(address))
      connection.setAttachment(wrapper)
      logger.info { "$address connected." }
      clientEventHandler.onClientConnected(wrapper)
    }

    onError = { _, e ->
      logger.error(e) { "onError" }
    }

    return this
  }

  private companion object {
    private val logger = Logger<WsTransportBuilder>()
  }
}
