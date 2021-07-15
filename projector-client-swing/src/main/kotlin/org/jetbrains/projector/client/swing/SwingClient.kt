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
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.jetbrains.projector.client.swing

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.jetbrains.projector.client.common.SingleRenderingSurfaceProcessor.Companion.shrinkByPaintEvents
import org.jetbrains.projector.client.common.SwingFontCache
import org.jetbrains.projector.client.common.misc.ImageCacher
import org.jetbrains.projector.client.common.protocol.KotlinxJsonToClientHandshakeDecoder
import org.jetbrains.projector.client.common.protocol.KotlinxJsonToServerHandshakeEncoder
import org.jetbrains.projector.client.common.protocol.KotlinxProtoBufToClientMessageDecoder
import org.jetbrains.projector.client.common.protocol.SerializationToServerMessageEncoder
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.handshake.*
import org.jetbrains.projector.common.protocol.toClient.*
import org.jetbrains.projector.common.protocol.toServer.ClientDisplaySetChangeEvent
import org.jetbrains.projector.util.logging.Logger
import sun.awt.DisplayChangedListener
import sun.java2d.SunGraphicsEnvironment
import java.awt.GraphicsEnvironment
import java.util.*
import javax.swing.SwingUtilities
import javax.swing.Timer

class SwingClient(val transport: ProjectorTransport, val windowManager: AbstractWindowManager<*>) {
  val logger = Logger<SwingClient>()
  val timer = Timer(100) {
    val requests = ImageCacher.extractImagesToRequest()
    if (requests.isNotEmpty())
      transport.send(SerializationToServerMessageEncoder.encode(requests))
  }.also { it.start() }

  init {
    GlobalScope.launch {
      try {
        baseConnectionFlow()
      } finally {
        timer.stop()
      }
    }

    val displayListener = object: DisplayChangedListener {
      override fun displayChanged() = sendDisplayChangeEvent()
      override fun paletteChanged() = sendDisplayChangeEvent()
    }

    val sunGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment() as? SunGraphicsEnvironment
    sunGraphicsEnvironment?.addDisplayChangedListener(displayListener)
    transport.onClosed.handle { _, _ ->
      SwingUtilities.invokeLater {
        sunGraphicsEnvironment?.removeDisplayChangedListener(displayListener)
      }
    }
  }

  fun sendDisplayChangeEvent() {
    transport.send(ClientDisplaySetChangeEvent(produceDisplayList()))
  }

  fun processEvent(serverEvent: ServerEvent) {
    Do exhaustive when(serverEvent) {
      is ServerImageDataReplyEvent -> ImageCacher.putImageData(serverEvent.imageId, serverEvent.imageData)
      is ServerPingReplyEvent -> logger.debug { "Received and discarded server ping reply event: $serverEvent" }
      is ServerClipboardEvent -> windowManager.handleClipboardEvent(serverEvent)
      is ServerWindowSetChangedEvent -> windowManager.windowSetUpdated(serverEvent)
      is ServerDrawCommandsEvent -> {
        val target = serverEvent.target
        Do exhaustive when(target) {
          is ServerDrawCommandsEvent.Target.Onscreen -> windowManager.doWindowDraw(target.windowId, serverEvent.drawEvents)
          is ServerDrawCommandsEvent.Target.Offscreen -> {
            val processor = ImageCacher.getOffscreenProcessor(target)
            val newEvents = serverEvent.drawEvents.shrinkByPaintEvents()
            val firstUnsuccessfulEvent = processor.processNew(newEvents)
            firstUnsuccessfulEvent?.let {
              logger.error { "Skipping drawing unsuccessful event for $target" }  // todo: support it
            }
          }
        }
      }
      is ServerCaretInfoChangedEvent -> logger.debug { "Received and discarded caret info event: $serverEvent" }
      is ServerMarkdownEvent -> windowManager.handleMarkdownEvent(serverEvent)
      is ServerWindowColorsEvent -> logger.debug { "Received and discarded color event: $serverEvent" }
    }
  }

  private suspend fun baseConnectionFlow() {
    transport.connect()

    transport.onOpen.await()

    val allScreens = produceDisplayList()

    val handshake = ToServerHandshakeEvent(
      commonVersion = COMMON_VERSION,
      commonVersionId = commonVersionList.indexOf(COMMON_VERSION),

      clientDoesWindowManagement = true,
      displays = allScreens,
      supportedToClientCompressions = listOf(CompressionType.NONE),
      supportedToClientProtocols = listOf(ProtocolType.KOTLINX_PROTOBUF),
      supportedToServerCompressions = listOf(CompressionType.NONE),
      supportedToServerProtocols = listOf(ProtocolType.KOTLINX_JSON)
    )

    transport.send("$HANDSHAKE_VERSION;${handshakeVersionList.indexOf(HANDSHAKE_VERSION)}")
    transport.send(KotlinxJsonToServerHandshakeEncoder.encode(handshake))

    val response = transport.messages.receive()
    val serverHandshake = KotlinxJsonToClientHandshakeDecoder.decode(response)

    Do exhaustive when (serverHandshake) {
      is ToClientHandshakeSuccessEvent -> {
        require(serverHandshake.toClientCompression == CompressionType.NONE)
        require(serverHandshake.toServerCompression == CompressionType.NONE)
        require(serverHandshake.toClientProtocol == ProtocolType.KOTLINX_PROTOBUF)
        require(serverHandshake.toServerProtocol == ProtocolType.KOTLINX_JSON)

        serverHandshake.fontDataHolders.forEach {
          SwingFontCache.registerServerFont(it.fontId.toInt(), Base64.getDecoder().decode(it.fontData.ttfBase64))
        }
      }
      is ToClientHandshakeFailureEvent -> error("Handshake failed: ${serverHandshake.reason}")
    }

    transport.send("Unused string meaning fonts loading is done")

    for (msg in transport.messages) {
      val commands = KotlinxProtoBufToClientMessageDecoder.decode(msg)
      SwingUtilities.invokeLater {
        val drawEvents = commands.filterIsInstance<ServerDrawCommandsEvent>()
        commands.forEach {
          if (it !is ServerDrawCommandsEvent)
            processEvent(it)
        }
        drawEvents.forEach {
          processEvent(it)
        }
      }
    }

    SwingUtilities.invokeLater {
      windowManager.windowSetUpdated(ServerWindowSetChangedEvent())
    }
  }

  private fun produceDisplayList() = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map {
    with(it.defaultConfiguration.bounds) {
      DisplayDescription(x, y, width, height, it.defaultConfiguration.defaultTransform.scaleX)
    }
  }
}
