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
import org.jetbrains.projector.common.protocol.toClient.MainWindow
import org.jetbrains.projector.server.core.util.setSsl
import org.jetbrains.projector.util.logging.Logger
import java.net.InetAddress
import java.nio.ByteBuffer

public class HttpWsServerBuilder(private val host: InetAddress, private val port: Int): WsTransportBuilder() {

  public lateinit var getMainWindows: () -> List<MainWindow>

  override fun build(): HttpWsServer {
    val wsServer =  object : HttpWsServer(host, port) {
      override fun getMainWindows(): List<MainWindow> = this@HttpWsServerBuilder.getMainWindows()
      override fun onError(connection: WebSocket?, e: Exception) = this@HttpWsServerBuilder.onError(connection, e)
      override fun onWsOpen(connection: WebSocket) = this@HttpWsServerBuilder.onWsOpen(connection)
      override fun onWsClose(connection: WebSocket) = this@HttpWsServerBuilder.onWsClose(connection)
      override fun onWsMessage(connection: WebSocket, message: String) = this@HttpWsServerBuilder.onWsMessageString(connection, message)
      override fun onWsMessage(connection: WebSocket, message: ByteBuffer) = this@HttpWsServerBuilder.onWsMessageByteBuffer(connection,
                                                                                                                            message)
    }

    val message = when (val hint = setSsl(wsServer::setWebSocketFactory)) {
      null -> "WebSocket SSL is disabled"
      else -> "WebSocket SSL is enabled: $hint"
    }

    val logger = Logger<HttpWsServerBuilder>()
    logger.info { message }

    return wsServer
  }
}
