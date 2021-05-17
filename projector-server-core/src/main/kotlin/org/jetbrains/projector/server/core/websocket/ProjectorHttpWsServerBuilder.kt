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
import org.jetbrains.projector.common.protocol.toClient.MainWindow
import org.jetbrains.projector.server.core.ProjectorHttpWsServer
import org.jetbrains.projector.server.core.util.setSsl
import org.jetbrains.projector.util.logging.Logger
import java.net.InetAddress
import java.nio.ByteBuffer

public class ProjectorHttpWsServerBuilder(private val host: InetAddress, private val port: Int): TransportBuilder() {

  public override fun build(): ProjectorHttpWsServer {
    val wsServer =  object : ProjectorHttpWsServer(host, port) {
      override fun getMainWindows(): List<MainWindow> {
        return this@ProjectorHttpWsServerBuilder.getMainWindow()
      }

      override fun onStart() {
        this@ProjectorHttpWsServerBuilder.onStart()
      }

      override fun onError(connection: WebSocket?, e: Exception) {
        this@ProjectorHttpWsServerBuilder.onError(connection, e)
      }

      override fun onWsOpen(connection: WebSocket) {
        this@ProjectorHttpWsServerBuilder.onWsOpen(connection)
      }

      override fun onWsClose(connection: WebSocket) {
        this@ProjectorHttpWsServerBuilder.onWsClose(connection)
      }

      override fun onWsMessage(connection: WebSocket, message: String) {
        this@ProjectorHttpWsServerBuilder.onWsMessageString(connection, message)
      }

      override fun onWsMessage(connection: WebSocket, message: ByteBuffer) {
        this@ProjectorHttpWsServerBuilder.onWsMessageByteBuffer(connection, message)
      }
    }

    val message = when (val hint = setSsl((wsServer)::setWebSocketFactory)) {
      null -> "WebSocket SSL is disabled"
      else -> "WebSocket SSL is enabled: $hint"
    }

    val logger = Logger<ProjectorHttpWsServerBuilder>()
    logger.info { message }

    return wsServer
  }
}
