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
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.jetbrains.projector.common.protocol.toServer.ClientInControlEvent
import org.jetbrains.projector.common.protocol.toServer.ClientOutControlEvent
import org.jetbrains.projector.common.protocol.toServer.GreetingControlEvent
import org.jetbrains.projector.common.protocol.toServer.KotlinxJsonClientEventSerializer
import org.jetbrains.projector.server.core.ClientWrapper
import org.jetbrains.projector.util.logging.Logger
import java.net.URI
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public abstract class HttpWsClient(
  private val relayUrl: String,
  private val serverId: String,
) : HttpWsTransport {

  private val clients: MutableMap<String, WebSocketClient> = mutableMapOf()

  private val controlWebSocket = object : WebSocketClient(URI("$relayUrl/register/$serverId"), Draft_6455()) {

    @Volatile
    private var wasInitialized: Boolean? = null
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()


    val wasStarted: Boolean
      get() {
        while (wasInitialized == null) {
          lock.withLock {
            if (wasInitialized == null) {
              condition.await()
            }
            else {
              return wasInitialized!!
            }
          }
        }

        return wasInitialized!!
      }

    override fun onOpen(handshakedata: ServerHandshake?) {

      lock.withLock {
        wasInitialized = true
        condition.signal()
      }
    }

    override fun onMessage(message: String?) {
      if (message.isNullOrEmpty()) {
        return
      }

      when (val event = KotlinxJsonClientEventSerializer.deserializeFromRelay(message)) {
        is ClientInControlEvent -> addClient(event.id)
        is ClientOutControlEvent -> removeClient(event.id)
        is GreetingControlEvent -> {}
      }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
      if (wasInitialized == null) {
        logger.info { "Closing control connection code: $code, reason: $reason" }
        logger.info { "Please check relay status." }
        lock.withLock {
          wasInitialized = false
          condition.signal()
        }
      }
      else {
        this@HttpWsClient.onWsClose(connection)
      }
    }

    override fun onError(ex: Exception) {
      this@HttpWsClient.onError(connection = connection, e = ex)

      if (ex is java.net.SocketException) {
        lock.withLock {
          wasInitialized = false
          condition.signal()
        }
      }
    }
  }

  public override val wasStarted: Boolean by controlWebSocket::wasStarted

  public override fun start() {
    logger.info { "Control socket connected to server with URI: ${controlWebSocket.uri}" }
    controlWebSocket.connect()
  }

  public override fun stop(timeoutMs: Int) {
    controlWebSocket.close()
  }

  public override fun forEachOpenedConnection(action: (client: ClientWrapper) -> Unit) {
    clients.values.filter(WebSocket::isOpen).forEach {
      val wrapper = it.getAttachment<ClientWrapper>() ?: return@forEachOpenedConnection
      action(wrapper)
    }
  }

  private fun removeClient(clientId: String) {
    clients.remove(clientId)
  }

  private fun addClient(clientId: String) {

    val clientWebSocket = object : WebSocketClient(URI("$relayUrl/accept/$serverId/$clientId")) {
      override fun onOpen(handshakedata: ServerHandshake?) {
        this@HttpWsClient.onWsOpen(connection)
      }

      override fun onMessage(message: String?) {

        if (message == null || message.isEmpty()) {
          return
        }

        this@HttpWsClient.onWsMessage(connection, message)
      }

      override fun onClose(code: Int, reason: String?, remote: Boolean) {
        this@HttpWsClient.onWsClose(connection)
      }

      override fun onError(ex: Exception) {
        this@HttpWsClient.onError(connection = connection, e = ex)
      }
    }
    clientWebSocket.connect()
    clients[clientId] = clientWebSocket
  }

  private companion object {
    private val logger = Logger<HttpWsClient>()
  }
}
