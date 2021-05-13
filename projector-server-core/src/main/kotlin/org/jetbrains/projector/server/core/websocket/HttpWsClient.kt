/*
 * Copyright (c) 2019-2021, JetBrains s.r.o. and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. JetBrains designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact JetBrains, Na Hrebenech II 1718/10, Prague, 14000, Czech Republic
 * if you need additional information or have any questions.
 */
package org.jetbrains.projector.server.core.websocket

import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.jetbrains.projector.common.protocol.toServer.ClientInControlEvent
import org.jetbrains.projector.common.protocol.toServer.ClientOutControlEvent
import org.jetbrains.projector.common.protocol.toServer.KotlinxJsonClientEventSerializer
import java.net.URI
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public abstract class HttpWsClient(
  private val relayUrl: String,
  private val serverId: String
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
      onStart()

      lock.withLock {
        wasInitialized = true
        condition.signal()
      }
    }

    override fun onMessage(message: String?) {
      if (message == null || message.isEmpty()) {
        return
      }

      when (val event = KotlinxJsonClientEventSerializer.deserializeFromRelay(message)) {
        is ClientInControlEvent -> addClient(event.id)
        is ClientOutControlEvent -> removeClient(event.id)
      }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
      this@HttpWsClient.onWsClose(connection)
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
    controlWebSocket.connect()
  }

  public override fun stop(timeout: Int) {
    controlWebSocket.close()
  }

  public override fun forEachOpenedConnection(action: (client: WebSocket) -> Unit) {
    clients.values.forEach {
      action(it)
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
}
