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
import org.jetbrains.projector.common.protocol.toClient.MainWindow
import org.jetbrains.projector.common.protocol.toClient.toJson
import org.jetbrains.projector.server.core.ClientWrapper
import org.jetbrains.projector.server.core.util.getWildcardHostAddress
import org.jetbrains.projector.util.logging.Logger
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


public class GetRequestResult(
  public val statusCode: Short,
  public val statusText: String,
  public val contentType: String,
  public val content: ByteArray,
)

private val ClientHandshake.isHttp: Boolean get() = this.getFieldValue("Upgrade").isNullOrBlank()

public abstract class HttpWsServer(host: InetAddress, port: Int) : HttpWsTransport {

  public constructor(port: Int) : this(getWildcardHostAddress(), port)

  public abstract fun getMainWindows(): List<MainWindow>

  public fun onGetRequest(path: String): GetRequestResult {
    val pathWithoutParams = path.substringBefore('?').substringBefore('#')

    if (pathWithoutParams == "/mainWindows") {
      val mainWindows = getMainWindows()
      val json = mainWindows.toJson().toByteArray()
      return GetRequestResult(
        statusCode = 200,
        statusText = "OK",
        contentType = "text/json",
        content = json,
      )
    }

    val resourceFileName = getResourceName(pathWithoutParams)
    val resourceBytes = getResource(resourceFileName)

    if (resourceBytes != null) {
      return GetRequestResult(
        statusCode = 200,
        statusText = "OK",
        contentType = resourceFileName.calculateContentType(),
        content = resourceBytes,
      )
    }

    return GetRequestResult(
      statusCode = 404,
      statusText = "Not found",
      contentType = "text/html",
      content = "<h1>404 Not found requested path: ${URLEncoder.encode(path, "utf-8")} (URL-encoded)</h1>".toByteArray(),
    )
  }

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
      val result = onGetRequest(handshakedata.getFieldValue(PATH_FIELD))

      val header = Charsetfunctions.asciiBytes(
        "HTTP/1.0 ${result.statusCode} ${result.statusText}\r\n" +
        "Mime-Version: 1.0\r\n" +
        "Content-Type: ${result.contentType}\r\n" +
        "Content-Length: ${result.content.size}\r\n" +
        "Connection: close\r\n" +
        "\r\n"
      )

      val bytebuffer = ByteBuffer.allocate(result.content.size + header.size)
      bytebuffer.put(header)
      bytebuffer.put(result.content)
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
    private val logger = Logger<HttpWsServer>()

    private const val PATH_FIELD = "X-My-Path"  // for draft

    private fun failNoFrames(): Nothing = throw UnsupportedOperationException("This draft doesn't work with frames")  // for draft
    private fun failNoClient(): Nothing = throw UnsupportedOperationException("This draft can't be used on a client")  // for draft

    private val HTTP_CONNECTION_ATTACHMENT = object {}

    private inline fun onlyForWs(connection: WebSocket, block: () -> Unit) {
      if (connection.getAttachment<Any?>() !== HTTP_CONNECTION_ATTACHMENT) {
        block()
      }
    }

    private fun String.calculateContentType() = when (this.substringAfterLast('.').lowercase()) {
      "css" -> "text/css"
      "html" -> "text/html"
      "js" -> "text/javascript"
      "png" -> "image/png"
      "svg" -> "image/svg+xml"
      "webmanifest" -> "application/manifest+json"
      else -> "application/octet-stream"
    }

    fun getResourceName(pathWithoutParams: String) = pathWithoutParams
      .let {
        // support paths from old docker instructions
        when (it.startsWith("/projector/")) {
          true -> it.removePrefix("/projector")
          false -> it
        }
      }
      .let {
        // support paths for PWA (start_url from webmanifest) – it's the name of repo so PWA could work for GH Pages too
        when (it.startsWith("/projector-client/")) {
          true -> it.removePrefix("/projector-client")
          false -> it
        }
      }
      .let {
        // support root
        when (it == "/") {
          true -> "/index.html"
          false -> it
        }
      }

    fun getResource(resourceFileName: String): ByteArray? {
      // todo: restrict going to upper dirs
      return this::class.java.getResource("/projector-client-web-distribution$resourceFileName")?.readBytes()
    }
  }

  private val webSocketServer = object : WebSocketServer(InetSocketAddress(host, port), listOf(HttpDraft(), Draft_6455())) {

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

    override fun onError(conn: WebSocket?, ex: Exception) {
      this@HttpWsServer.onError(connection = conn, e = ex)

      if (ex is java.net.SocketException) {
        lock.withLock {
          wasInitialized = false
          condition.signal()
        }
      }
    }

    override fun onStart() {
      logger.info { "Server started on host $host and port $port" }

      lock.withLock {
        wasInitialized = true
        condition.signal()
      }
    }
  }

  override val wasStarted: Boolean by webSocketServer::wasStarted

  override fun start() {
    webSocketServer.start()
  }

  override fun stop(timeoutMs: Int) {
    webSocketServer.stop(timeoutMs)
  }

  override fun forEachOpenedConnection(action: (client: ClientWrapper) -> Unit) {
    webSocketServer.connections.filter(WebSocket::isOpen).forEach {
      val wrapper = it.getAttachment<ClientWrapper>() ?: return@forEachOpenedConnection
      action(wrapper)
    }
  }

  public fun setWebSocketFactory(factory: WebSocketServerFactory) {
    webSocketServer.setWebSocketFactory(factory)
  }
}
