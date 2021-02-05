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
package org.jetbrains.projector.client.swing

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.jetbrains.projector.util.logging.Logger
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class WebSocketTransport(uri: URI) : WebSocketClient(uri), ProjectorTransport {
  private val logger = Logger<WebSocketTransport>()

  override val onOpen = CompletableFuture<Unit>()
  override val onClosed = CompletableFuture<Unit>()
  override val connectionTime: Long = System.currentTimeMillis()

  override val messages = Channel<ByteArray>()

  override fun onOpen(handshakedata: ServerHandshake?) {
    logger.info { "Websocket connected" }
    onOpen.complete(Unit)
  }

  override fun onMessage(bytes: ByteBuffer?) {
    bytes ?: return
    val array = ByteArray(bytes.capacity()) { bytes[it] }
    messages.sendBlocking(array)
  }

  override fun onMessage(message: String?) {
    error("Remote sent a string message, this is not supported; $message")
  }

  override fun onClose(code: Int, reason: String?, remote: Boolean) {
    logger.info { "Websocket closed with code $code/$reason" }
    onClosed.complete(Unit)
    onOpen.cancel(false)
    messages.close()
  }

  override fun onError(ex: Exception?) {
    logger.error(ex) { "Error in websocket" }
    messages.close(ex)
  }
}
