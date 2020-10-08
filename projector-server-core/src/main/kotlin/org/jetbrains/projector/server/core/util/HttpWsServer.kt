/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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
package org.jetbrains.projector.server.core.util

import org.java_websocket.WebSocket
import org.java_websocket.WebSocketImpl
import org.java_websocket.WebSocketServerFactory
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.enums.CloseHandshakeType
import org.java_websocket.enums.HandshakeState
import org.java_websocket.framing.Framedata
import org.java_websocket.handshake.*
import org.java_websocket.server.WebSocketServer
import org.java_websocket.util.Charsetfunctions
import java.net.InetSocketAddress
import java.nio.ByteBuffer

private val ClientHandshake.isHttp: Boolean get() = this.getFieldValue("Upgrade").isNullOrBlank()

public abstract class HttpWsServer(port: Int) {

  private inner class HttpDraft : Draft() {

    override fun acceptHandshakeAsServer(handshakedata: ClientHandshake): HandshakeState {
      return when (handshakedata.isHttp) {
        true -> HandshakeState.MATCHED
        false -> HandshakeState.NOT_MATCHED
      }
    }

    override fun postProcessHandshakeResponseAsServer(request: ClientHandshake, response: ServerHandshakeBuilder): HandshakeBuilder {
      response.put(PATH_FIELD, request.resourceDescriptor)
      return response
    }

    override fun createHandshake(handshakedata: Handshakedata, withcontent: Boolean): List<ByteBuffer> {
      val content = this@HttpWsServer.onGetRequest(handshakedata.getFieldValue(PATH_FIELD))

      val header = Charsetfunctions.asciiBytes(
        "HTTP/1.0 200 OK\r\n" +
        "Mime-Version: 1.0\r\n" +
        "Content-Type: text/html\r\n" +
        "Content-Length: ${content.size}\r\n" +
        "Connection: close\r\n" +
        "\r\n"
      )

      val bytebuffer = ByteBuffer.allocate(content.size + header.size)
      bytebuffer.put(header)
      bytebuffer.put(content)
      bytebuffer.flip()
      return listOf(bytebuffer)
    }

    override fun getCloseHandshakeType(): CloseHandshakeType {
      return CloseHandshakeType.NONE
    }

    override fun copyInstance(): Draft {
      return this
    }

    override fun reset() {
      // nop
    }

    override fun translateFrame(buffer: ByteBuffer) = failNoFrames()
    override fun processFrame(webSocketImpl: WebSocketImpl, frame: Framedata) = failNoFrames()
    override fun createBinaryFrame(framedata: Framedata) = failNoFrames()
    override fun createFrames(binary: ByteBuffer, mask: Boolean) = failNoFrames()
    override fun createFrames(text: String, mask: Boolean) = failNoFrames()

    override fun postProcessHandshakeRequestAsClient(request: ClientHandshakeBuilder) = failNoClient()
    override fun acceptHandshakeAsClient(request: ClientHandshake, response: ServerHandshake) = failNoClient()
  }

  private companion object {

    private const val PATH_FIELD = "X-My-Path"  // for draft

    private fun failNoFrames(): Nothing = throw UnsupportedOperationException("This draft doesn't work with frames")  // for draft
    private fun failNoClient(): Nothing = throw UnsupportedOperationException("This draft can't be used on a client")  // for draft

    private val HTTP_CONNECTION_ATTACHMENT = object {}

    private inline fun onlyForWs(connection: WebSocket, block: () -> Unit) {
      if (connection.getAttachment<Any?>() !== HTTP_CONNECTION_ATTACHMENT) {
        block()
      }
    }
  }

  private val webSocketServer = object : WebSocketServer(InetSocketAddress(port), listOf(HttpDraft(), Draft_6455())) {

    init {
      isReuseAddr = true
      isTcpNoDelay = true
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
      when (handshake.isHttp) {
        true -> {
          conn.setAttachment(HTTP_CONNECTION_ATTACHMENT)
          conn.close()
        }
        false -> this@HttpWsServer.onWsOpen(conn)
      }
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) = onlyForWs(conn) {
      this@HttpWsServer.onWsClose(conn)
    }

    override fun onMessage(conn: WebSocket, message: String) = onlyForWs(conn) {
      this@HttpWsServer.onWsMessage(conn, message)
    }

    override fun onMessage(conn: WebSocket, message: ByteBuffer) {
      this@HttpWsServer.onWsMessage(conn, message)
    }

    override fun onError(conn: WebSocket?, ex: Exception) = this@HttpWsServer.onError(connection = conn, e = ex)
    override fun onStart() = this@HttpWsServer.onStart()
  }

  public fun start() {
    webSocketServer.start()
  }

  public fun stop(timeout: Int) {
    webSocketServer.stop(timeout)
  }

  public fun forEachOpenedConnection(action: (client: WebSocket) -> Unit) {
    webSocketServer.connections.filter(WebSocket::isOpen).forEach(action)
  }

  public fun setWebSocketFactory(factory: WebSocketServerFactory) {
    webSocketServer.setWebSocketFactory(factory)
  }

  public abstract fun onStart()
  public abstract fun onError(connection: WebSocket?, e: Exception)
  public abstract fun onWsOpen(connection: WebSocket)
  public abstract fun onWsClose(connection: WebSocket)
  public abstract fun onWsMessage(connection: WebSocket, message: String)
  public abstract fun onWsMessage(connection: WebSocket, message: ByteBuffer)
  public abstract fun onGetRequest(path: String): ByteArray
}
